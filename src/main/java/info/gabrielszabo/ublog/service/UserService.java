package info.gabrielszabo.ublog.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import info.gabrielszabo.ublog.config.Config;
import info.gabrielszabo.ublog.domain.User;
import info.gabrielszabo.ublog.service.log.LogService;

public class UserService {

    private static final Pattern VALID_USERNAME = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final String userFolder;
    private final ConcurrentMap<String, User> userCache = new ConcurrentHashMap<>();
    private final LogService log; 

    public UserService(LogService log) {
        this.userFolder = Config.USER_FOLDER.value();
        this.log = log;
    }

    public synchronized Optional<User> getUser(String username) {
        if (!isValidUsername(username)) {
            return Optional.empty();
        }
        User cachedUser = userCache.get(username);
        if (cachedUser != null) {
            log.logInfo("User found in cache: " + username);
            return Optional.of(cachedUser);
        }
        File targetFile = new File(userFolder, username + ".properties");
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(targetFile)) {
            props.load(fis);
        } catch (IOException e) {
            log.logError("Failed to load user properties for: " + username + e.getLocalizedMessage());
            return Optional.empty();
        }
        User user = new User(props);
        if (user.getStatus() == null || !user.getStatus().equals("active")) {
            log.logWarning("User is not active: " + username);
            return Optional.empty();
        }
        user.setUsername(username);
        userCache.put(username, user);

        log.logInfo("User loaded from files and cached: " + username);

        return Optional.of(user);
    }

    public boolean isEditor(String username) {
        Optional<User> userOpt = getUser(username);
        return userOpt.isPresent() && Boolean.TRUE.equals(userOpt.get().getEditor());
    }

    public String getFullName(String username) {
        Optional<User> userOpt = getUser(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return user.getName();
        }
        return "";
    }

    public boolean updatePassword(String username, String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        return updateProperty(username, "password", encodedPassword);
    }

    /** Stores the token hash, or removes the token when tokenHash is null. */
    public boolean updateToken(String username, String tokenHash) {
        return updateProperty(username, "token", tokenHash);
    }

    private synchronized boolean updateProperty(String username, String key, String value) {
        if (!isValidUsername(username)) {
            return false;
        }
        File targetFile = new File(userFolder, username + ".properties");
        Properties props = new Properties();
        try (FileInputStream input = new FileInputStream(targetFile)) {
            props.load(input);
        } catch (IOException e) {
            log.logError("Failed to load user properties for update");
            return false;
        }
        if (value == null) {
            props.remove(key);
        } else {
            props.setProperty(key, value);
        }
        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile(targetFile.toPath().getParent(), targetFile.getName(), ".tmp");
            try (FileOutputStream output = new FileOutputStream(temporaryFile.toFile())) {
                props.store(output, null);
            }
            try {
                Files.move(temporaryFile, targetFile.toPath(), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                Files.move(temporaryFile, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.logError("Failed to persist user properties");
            return false;
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    log.logWarning("Failed to remove temporary user file");
                }
            }
        }
        userCache.remove(username);
        return true;
    }

    private boolean isValidUsername(String username) {
        return username != null && VALID_USERNAME.matcher(username).matches();
    }
    
}
