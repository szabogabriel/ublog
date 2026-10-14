package info.gabrielszabo.ublog.domain;

public class SessionContext {
    private Long sessionId = 0L;
    private boolean isLoggedIn = false;
    private boolean isInitialLogin = false;
    private boolean isLoggingOut = false;

    public SessionContext() {

    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setLoggedIn(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
    }

    public boolean isInitialLogin() {
        return isInitialLogin;
    }

    public void setInitialLogin(boolean isInitialLogin) {
        this.isInitialLogin = isInitialLogin;
    }

    public boolean isLoggingOut() {
        return isLoggingOut;
    }

    public void setLoggingOut(boolean isLoggingOut) {
        this.isLoggingOut = isLoggingOut;
    }
}
