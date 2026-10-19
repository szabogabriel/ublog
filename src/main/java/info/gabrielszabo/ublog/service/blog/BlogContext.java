package info.gabrielszabo.ublog.service.blog;
import info.gabrielszabo.ublog.domain.User;
public interface BlogContext {

    void addBlogEntryContentToContext(String[] pathParts);
    void setRequestedSingleEntry(String entryName);

    void addSuccessMessage(String message);
    void addInfoMessage(String message);
    void addErrorMessage(String message);

    void addIndexToContext();

    void setIsLogin();
    void setIsUserDetails();
    void setIsCreateTopic();

    void setCsrfToken(String csrfToken);
    void setUserDetails(User user);
    void setUsername(String username);
    void setName(String name);
    void setLoggedIn(boolean loginSuccessful);
    void setInitialLogin();
    void setLoggingOut();

    boolean isLoggedIn();
    boolean isLoggingOut();
    boolean isInitialLogin();
    boolean isCreateTopic();

    String getUsername();
    String getName();
    
}
