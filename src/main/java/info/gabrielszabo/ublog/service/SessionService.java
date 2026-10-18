package info.gabrielszabo.ublog.service;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SessionService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CLEANUP_INTERVAL = 256;
    private final Long sessionTimeout;
    private final ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final AtomicInteger operationCounter = new AtomicInteger();

    public SessionService() {
        this.sessionTimeout = Long.parseLong(System.getenv().getOrDefault("SESSION_TIMEOUT", "60")) * 60 * 1000;
    }

    public String createSession(String username) {
        maybeCleanupExpiredSessions(System.currentTimeMillis());
        String sessionId;
        long now = System.currentTimeMillis();
        do {
            sessionId = generateSessionId();
        } while (sessions.putIfAbsent(sessionId, new Session(username, now, generateCsrfToken())) != null);
        return sessionId;
    }

    public void invalidateSession(String sessionId) {
        maybeCleanupExpiredSessions(System.currentTimeMillis());
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    public boolean isSessionValid(String sessionId) {
        return getValidSession(sessionId) != null;
    }

    public void refreshSession(String sessionId) {
        if (sessionId == null) {
            return;
        }
        long now = System.currentTimeMillis();
        maybeCleanupExpiredSessions(now);
        sessions.computeIfPresent(sessionId, (id, session) -> session.isValidAt(now, sessionTimeout)
                ? new Session(session.username, now, session.csrfToken) : null);
    }

    public String getUsernameForSession(String sessionId) {
        Session session = getValidSession(sessionId);
        return session == null ? null : session.username;
    }

    public String getCsrfToken(String sessionId) {
        Session session = getValidSession(sessionId);
        return session == null ? null : session.csrfToken;
    }

    public boolean isCsrfTokenValid(String sessionId, String csrfToken) {
        if (csrfToken == null || csrfToken.isBlank()) {
            return false;
        }
        Session session = getValidSession(sessionId);
        return session != null && session.csrfToken.equals(csrfToken);
    }

    private Session getValidSession(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        maybeCleanupExpiredSessions(now);
        return sessions.computeIfPresent(sessionId, (id, session) ->
                session.isValidAt(now, sessionTimeout) ? session : null);
    }

    private void maybeCleanupExpiredSessions(long now) {
        if ((operationCounter.incrementAndGet() % CLEANUP_INTERVAL) == 0) {
            sessions.forEach((sessionId, session) -> {
                if (!session.isValidAt(now, sessionTimeout)) {
                    sessions.remove(sessionId, session);
                }
            });
        }
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

    private String generateCsrfToken() {
        byte[] randomBytes = new byte[32];
        RANDOM.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }

    private static final class Session {
        private final String username;
        private final long lastAccessTime;
        private final String csrfToken;

        private Session(String username, long lastAccessTime, String csrfToken) {
            this.username = username;
            this.lastAccessTime = lastAccessTime;
            this.csrfToken = csrfToken;
        }

        private boolean isValidAt(long now, long timeout) {
            return now - lastAccessTime <= timeout;
        }
    }

}
