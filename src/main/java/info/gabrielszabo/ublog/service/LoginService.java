package info.gabrielszabo.ublog.service;

import java.util.Optional;

import info.gabrielszabo.ublog.domain.User;
import info.gabrielszabo.ublog.service.log.LogService;

public class LoginService {

    private final UserService userService;
    private final LogService log;

    public LoginService(UserService userService, LogService log) {
        this.userService = userService;
        this.log = log;
    }

    public boolean login(String username, String password) {
        Optional<User> userOpt = userService.getUser(username);

        if (userOpt.isEmpty()) {
            log.logError("User not found: " + username);
            return false;
        }

        User user = userOpt.get();
        boolean success = user.getPassword().equals(password);

        if (success) {
            log.logInfo("User logged in successfully: " + username);
        } else {
            log.logError("Failed login attempt for user: " + username);
        }

        return success;
    }
    
}
