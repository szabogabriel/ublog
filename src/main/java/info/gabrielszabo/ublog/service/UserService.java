package info.gabrielszabo.ublog.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.WeakHashMap;

import info.gabrielszabo.ublog.config.Config;
import info.gabrielszabo.ublog.domain.User;
import info.gabrielszabo.ublog.service.log.LogService;

public class UserService {

    private final String userFolder;
    private final Map<String, User> userCache = new WeakHashMap<>();
    private final LogService log; 

    public UserService(LogService log) {
        this.userFolder = Config.USER_FOLDER.value();
        this.log = log;
    }

    public Optional<User> getUser(String username) {
        if (userCache.containsKey(username)) {
            log.logInfo("User found in cache: " + username);
            return Optional.of(userCache.get(username));
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
        userCache.put(username, user);

        log.logInfo("User loaded from files and cached: " + username);

        return Optional.of(user);
    }

    public String getFullName(String username) {
        Optional<User> userOpt = getUser(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return user.getName();
        }
        return "";
    }
    
}
