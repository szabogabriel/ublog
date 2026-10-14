package info.gabrielszabo.ublog.service;

import java.util.HashMap;
import java.util.Map;

import info.gabrielszabo.ublog.service.log.LogService;

public class SessionService {

    private final Long sessionTimeout;
    private final Map<Long, Long> sessions = new HashMap<>();
    private final Map<Long, String> sessionUsernames = new HashMap<>();
    private final LogService log;

    public SessionService(LogService log) {
        this.sessionTimeout = Long.parseLong(System.getenv().getOrDefault("SESSION_TIMEOUT", "60")) * 60 * 1000;
        this.log = log;
    }

    public Long createSession(String username) {
        long ret = generateSessionId();
        
        sessions.put(ret, System.currentTimeMillis());
        sessionUsernames.put(ret, username);
        return ret;
    }

    public void invalidateSession(Long sessionId) {
        if (sessions.containsKey(sessionId)) {
            sessions.remove(sessionId);
            sessionUsernames.remove(sessionId);
        }
    }

    public boolean isSessionValid(Long sessionId) {
        boolean isValid = false;
        if (sessions.containsKey(sessionId)) {
            long sessionTimestamp = sessions.get(sessionId);
            isValid = (System.currentTimeMillis() - sessionTimestamp) <= sessionTimeout;
        }

            return isValid;
    }

    public void refreshSession(Long sessionId) {
        if (sessions.containsKey(sessionId)) {
            long newTimestamp = System.currentTimeMillis();
            sessions.put(sessionId, newTimestamp);
            log.logInfo("Refreshed session: " + sessionId);
        }
    }

    public String getUsernameForSession(Long sessionId) {
        if (sessionId == null || !sessions.containsKey(sessionId)) {
            return null;
        }
        return sessionUsernames.get(sessionId);
    }

    private long generateSessionId() {
        long ret = 0L;
        do {
            long currentMillis = System.currentTimeMillis();
            long reversedNano = reverseNumber(System.nanoTime());
            ret = currentMillis ^ reversedNano;
        } while (sessions.containsKey(ret));
        return ret;
    }

    private long reverseNumber(long value) {
        long reversed = 0;
        while (value != 0) {
            reversed = reversed * 10 + (value % 10);
            value /= 10;
        }
        return reversed;
    }

}
