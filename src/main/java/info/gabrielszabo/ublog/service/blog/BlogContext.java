package info.gabrielszabo.ublog.service.blog;

public interface BlogContext {

    void addBlogEntryContentToContext(String[] pathParts);
    void setRequestedSingleEntry(String entryName);

    void addIndexToContext();
    void setIsLogin();

    void setUsername(String username);
    void setName(String name);
    void setLoggedIn(boolean loginSuccessful);
    void setInitialLogin();
    void setLoggingOut();

    boolean isLoggedIn();
    boolean isLoggingOut();
    boolean isInitialLogin();
    String getUsername();
    String getName();
    
    
}
