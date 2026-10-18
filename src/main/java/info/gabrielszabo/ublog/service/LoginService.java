package info.gabrielszabo.ublog.service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import info.gabrielszabo.ublog.domain.User;
import info.gabrielszabo.ublog.service.log.LogService;

public class LoginService {

    private static final int HASH_ITERATIONS = 5;
    private static final int HASH_MEMORY_KIB = 7168;
    private static final int HASH_PARALLELISM = 1;
    private static final int MAX_FAILURES = 5;
    private static final int MAX_FAILURE_ENTRIES = 10_000;
    private static final long FAILURE_WINDOW_MILLIS = 30 * 60 * 1000L;
    private static final int CLEANUP_INTERVAL = 256;
    private static final String ARGON2ID_PREFIX = "$argon2id$";

    private final UserService userService;
    private final LogService log;
    private final ConcurrentMap<String, FailureWindow> failures = new ConcurrentHashMap<>();
    private final AtomicInteger cleanupCounter = new AtomicInteger();

    public LoginService(UserService userService, LogService log) {
        this.userService = userService;
        this.log = log;
    }

    public boolean login(String username, String password) {
        long now = System.currentTimeMillis();
        if (username == null || username.isBlank() || password == null || isRateLimited(username, now)) {
            return false;
        }

        Optional<User> userOpt = userService.getUser(username);

        if (userOpt.isEmpty()) {
            recordFailure(username, now);
            return false;
        }

        User user = userOpt.get();
        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            recordFailure(username, now);
            return false;
        }

        boolean success;
        Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
        char[] passwordChars = password.toCharArray();
        try {
            success = verifyStoredPassword(argon2, storedPassword, password, passwordChars);
            if (success && !storedPassword.startsWith(ARGON2ID_PREFIX)) {
                success = userService.updatePassword(username, hashPassword(argon2, passwordChars));
            }
        } catch (IllegalArgumentException e) {
            success = false;
        } finally {
            argon2.wipeArray(passwordChars);
        }

        if (success) {
            failures.remove(username);
            log.logInfo("User logged in successfully: " + username);
        } else {
            recordFailure(username, now);
            log.logError("Failed login attempt for user: " + username);
        }

        return success;
    }

    public PasswordChangeResult changePassword(String username, String currentPassword, String newPassword) {
        if (username == null || username.isBlank() || currentPassword == null || currentPassword.isEmpty()
                || newPassword == null || newPassword.isEmpty()) {
            return PasswordChangeResult.INVALID_REQUEST;
        }

        Optional<User> userOpt = userService.getUser(username);
        if (userOpt.isEmpty()) {
            return PasswordChangeResult.USER_NOT_FOUND;
        }

        String storedPassword = userOpt.get().getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            return PasswordChangeResult.INVALID_CURRENT_PASSWORD;
        }

        Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
        char[] currentPasswordChars = currentPassword.toCharArray();
        char[] newPasswordChars = newPassword.toCharArray();
        try {
            if (!verifyStoredPassword(argon2, storedPassword, currentPassword, currentPasswordChars)) {
                return PasswordChangeResult.INVALID_CURRENT_PASSWORD;
            }
            if (!userService.updatePassword(username, hashPassword(argon2, newPasswordChars))) {
                return PasswordChangeResult.UPDATE_FAILED;
            }
            return PasswordChangeResult.SUCCESS;
        } catch (IllegalArgumentException e) {
            return PasswordChangeResult.INVALID_CURRENT_PASSWORD;
        } finally {
            argon2.wipeArray(currentPasswordChars);
            argon2.wipeArray(newPasswordChars);
        }
    }

    private boolean verifyStoredPassword(Argon2 argon2, String storedPassword, String providedPassword,
            char[] providedPasswordChars) {
        if (storedPassword.startsWith(ARGON2ID_PREFIX)) {
            return argon2.verify(storedPassword, providedPasswordChars);
        }
        return storedPassword.equals(providedPassword);
    }

    private String hashPassword(Argon2 argon2, char[] passwordChars) {
        return argon2.hash(HASH_ITERATIONS, HASH_MEMORY_KIB, HASH_PARALLELISM, passwordChars);
    }

    private boolean isRateLimited(String username, long now) {
        cleanupExpiredEntriesIfDue(now);
        FailureWindow window = failures.get(username);
        if (window == null) {
            return failures.size() >= MAX_FAILURE_ENTRIES;
        }
        if (now - window.windowStartedAt >= FAILURE_WINDOW_MILLIS) {
            failures.remove(username, window);
            return false;
        }
        return window.failures >= MAX_FAILURES;
    }

    private synchronized void recordFailure(String username, long now) {
        cleanupExpiredEntriesIfDue(now);
        if (!failures.containsKey(username) && failures.size() >= MAX_FAILURE_ENTRIES) {
            return;
        }
        failures.compute(username, (key, existing) -> {
            if (existing == null || now - existing.windowStartedAt >= FAILURE_WINDOW_MILLIS) {
                return new FailureWindow(now, 1);
            }
            return new FailureWindow(existing.windowStartedAt, existing.failures + 1);
        });
    }

    private void cleanupExpiredEntriesIfDue(long now) {
        if ((cleanupCounter.incrementAndGet() % CLEANUP_INTERVAL) == 0) {
            failures.forEach((username, window) -> {
                if (now - window.windowStartedAt >= FAILURE_WINDOW_MILLIS) {
                    failures.remove(username, window);
                }
            });
        }
    }

    private static final class FailureWindow {
        private final long windowStartedAt;
        private final int failures;

        private FailureWindow(long windowStartedAt, int failures) {
            this.windowStartedAt = windowStartedAt;
            this.failures = failures;
        }
    }

    public enum PasswordChangeResult {
        SUCCESS,
        INVALID_REQUEST,
        USER_NOT_FOUND,
        INVALID_CURRENT_PASSWORD,
        UPDATE_FAILED
    }
    
}
