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
        do {
            sessionId = generateSessionId();
        } while (sessions.putIfAbsent(sessionId, new Session(username, System.currentTimeMillis())) != null);
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
                ? new Session(session.username, now) : null);
    }

    public String getUsernameForSession(String sessionId) {
        Session session = getValidSession(sessionId);
        return session == null ? null : session.username;
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

    private static final class Session {
        private final String username;
        private final long lastAccessTime;

        private Session(String username, long lastAccessTime) {
            this.username = username;
            this.lastAccessTime = lastAccessTime;
        }

        private boolean isValidAt(long now, long timeout) {
            return now - lastAccessTime <= timeout;
        }
    }

}
