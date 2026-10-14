package info.gabrielszabo.ublog.service;

import java.util.HashMap;
import java.util.Map;

import info.gabrielszabo.ublog.server.http.Cookie;
import info.gabrielszabo.ublog.service.log.LogService;

public class SessionService {

    private final Long sessionTimeout;
    private final Map<String, Long> sessions = new HashMap<>();
    private final LogService log;

    public SessionService(LogService log) {
        this.sessionTimeout = Long.parseLong(System.getenv().getOrDefault("SESSION_TIMEOUT", "60")) * 60 * 1000;
        this.log = log;
    }

    public void  createSession(String userName) {
        invalidateSession(userName);
        long sessionId = System.currentTimeMillis();
        sessions.put(userName, sessionId);
    }

    public Cookie getSessionCookie(String userName) {
        log.logTrace("Creating session cookie for " + userName);
        if (sessions.containsKey(userName)) {
            Cookie ret = new Cookie("session", userName);
            log.logTrace("Cookie created: " + ret);
            return ret;
        }
        return null;
    }

    public Cookie getSessionInvalidationCookie(String userName) {
        return new Cookie("session", "", 0);
    }

    public void invalidateSession(String userName) {
        if (sessions.containsKey(userName)) {
            sessions.remove(userName);
            log.logInfo("Invalidated session for user: " + userName);
        }
    }

    public boolean isSessionValid(String userName) {
        boolean isValid = false;
        if (sessions.containsKey(userName)) {
            long sessionTimestamp = sessions.get(userName);
            isValid = (System.currentTimeMillis() - sessionTimestamp) <= sessionTimeout;
        }

        log.logInfo("Session validity check for user: " + userName + " - " + isValid);
        return isValid;
    }

    public void refreshSession(String userName) {
        if (sessions.containsKey(userName)) {
            long sessionId = System.currentTimeMillis();
            sessions.put(userName, sessionId);
            log.logInfo("Refreshed session for user: " + userName);
        }
    }
    
}
