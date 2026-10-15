package info.gabrielszabo.ublog.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Optional;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import info.gabrielszabo.ublog.domain.User;
import info.gabrielszabo.ublog.service.log.LogService;

public class LoginService {

    private static final int HASH_ITERATIONS = 5;
    private static final int HASH_MEMORY_KIB = 7168;
    private static final int HASH_PARALLELISM = 1;
    private static final int MAX_FAILURES = 5;
    private static final long FAILURE_WINDOW_MILLIS = 30 * 60 * 1000L;
    private static final String ARGON2ID_PREFIX = "$argon2id$";

    private final UserService userService;
    private final LogService log;
    private final ConcurrentMap<String, FailureWindow> failures = new ConcurrentHashMap<>();

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
            if (storedPassword.startsWith(ARGON2ID_PREFIX)) {
                success = argon2.verify(storedPassword, passwordChars);
            } else {
                success = storedPassword.equals(password);
                if (success) {
                    String encodedPassword = argon2.hash(HASH_ITERATIONS, HASH_MEMORY_KIB,
                            HASH_PARALLELISM, passwordChars);
                    success = userService.updatePassword(username, encodedPassword);
                }
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

    private boolean isRateLimited(String username, long now) {
        FailureWindow window = failures.get(username);
        if (window == null) {
            return false;
        }
        if (now - window.windowStartedAt >= FAILURE_WINDOW_MILLIS) {
            failures.remove(username, window);
            return false;
        }
        return window.failures >= MAX_FAILURES;
    }

    private void recordFailure(String username, long now) {
        failures.compute(username, (key, existing) -> {
            if (existing == null || now - existing.windowStartedAt >= FAILURE_WINDOW_MILLIS) {
                return new FailureWindow(now, 1);
            }
            return new FailureWindow(existing.windowStartedAt, existing.failures + 1);
        });
    }

    private static final class FailureWindow {
        private final long windowStartedAt;
        private final int failures;

        private FailureWindow(long windowStartedAt, int failures) {
            this.windowStartedAt = windowStartedAt;
            this.failures = failures;
        }
    }
    
}
