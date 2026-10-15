package info.gabrielszabo.ublog.service;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

public class SessionService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final Long sessionTimeout;
    private final Map<String, Long> sessions = new HashMap<>();
    private final Map<String, String> sessionUsernames = new HashMap<>();

    public SessionService() {
        this.sessionTimeout = Long.parseLong(System.getenv().getOrDefault("SESSION_TIMEOUT", "60")) * 60 * 1000;
    }

    public String createSession(String username) {
        String ret = null;
        
        do {
            ret = generateSessionId();
        } while (sessions.containsKey(ret));
        
        sessions.put(ret, System.currentTimeMillis());
        sessionUsernames.put(ret, username);
        return ret;
    }

    public void invalidateSession(String sessionId) {
        if (sessions.containsKey(sessionId)) {
            sessions.remove(sessionId);
            sessionUsernames.remove(sessionId);
        }
    }

    public boolean isSessionValid(String sessionId) {
        boolean isValid = false;
        if (sessions.containsKey(sessionId)) {
            long sessionTimestamp = sessions.get(sessionId);
            isValid = (System.currentTimeMillis() - sessionTimestamp) <= sessionTimeout;
        }

            return isValid;
    }

    public void refreshSession(String sessionId) {
        if (sessions.containsKey(sessionId)) {
            long newTimestamp = System.currentTimeMillis();
            sessions.put(sessionId, newTimestamp);
        }
    }

    public String getUsernameForSession(String sessionId) {
        if (sessionId == null || !sessions.containsKey(sessionId)) {
            return null;
        }
        return sessionUsernames.get(sessionId);
    }

    private String generateSessionId() {
        byte[] randomBytes = new byte[16];
        String ret;
        do {
            RANDOM.nextBytes(randomBytes);
            ret = HexFormat.of().formatHex(randomBytes);
        } while (sessions.containsKey(ret));
        return ret;
    }

}
