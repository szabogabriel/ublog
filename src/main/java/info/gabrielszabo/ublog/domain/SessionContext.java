package info.gabrielszabo.ublog.domain;

public class SessionContext {
    private String sessionId;
    private boolean isLoggedIn = false;
    private boolean isEditor = false;
    private boolean isInitialLogin = false;
    private boolean isLoggingOut = false;
    private boolean isImageRequest = false;

    public SessionContext() {

    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
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

    public boolean isEditor() {
        return isEditor;
    }

    public void setEditor(boolean isEditor) {
        this.isEditor = isEditor;
    }

    public boolean isImageRequest() {
        return isImageRequest;
    }

    public void setImageRequest(boolean isImageRequest) {
        this.isImageRequest = isImageRequest;
    }
}
