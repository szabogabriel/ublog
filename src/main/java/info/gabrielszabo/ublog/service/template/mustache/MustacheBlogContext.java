package info.gabrielszabo.ublog.service.template.mustache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import info.gabrielszabo.ublog.content.ContentProvider;
import info.gabrielszabo.ublog.domain.Category;
import info.gabrielszabo.ublog.domain.EntryDescriptor;
import info.gabrielszabo.ublog.domain.User;
import info.gabrielszabo.ublog.service.blog.BlogContext;

public class MustacheBlogContext implements BlogContext{
    private static final String CONTENT = "DATA";
    private static final String LOGIN = "LOGIN";
    private static final String TITLE = "PAGE_TITLE";
    private static final String BLOG_ENTRIES = "ENTRIES";
    private static final String BLOG_CATEGORIES = "TOPICS";
    private static final String QUICK_LINKS = "QUICK_LINKS";
    private static final String USERNAME = "USERNAME";
    private static final String NAME = "NAME";
    private static final String LOGGED_IN = "LOGGED_IN";

    private static final String USER_DETAILS = "USER_DETAILS";
    private static final String USER_USERNAME = "USER_USERNAME";
    private static final String USER_NAME = "USER_NAME";

    private static final String CSRF_TOKEN = "CSRF_TOKEN";
    private static final String TEMPLATE_PROPERTIES = "TEMPLATE_PROPERTIES";
    private static final String HAS_TEMPLATE_PROPERTIES = "HAS_TEMPLATE_PROPERTIES";
    
    private static final String SUCCESS_MESSAGE = "SUCCESS_MESSAGE";
    private static final String INFO_MESSAGE = "INFO_MESSAGE";
    private static final String ERROR_MESSAGE = "ERROR_MESSAGE";
    private static final String HAS_SUCCESS_MESSAGE = "HAS_SUCCESS_MESSAGE";
    private static final String HAS_INFO_MESSAGE = "HAS_INFO_MESSAGE";
    private static final String HAS_ERROR_MESSAGE = "HAS_ERROR_MESSAGE";

    private static final String ENTRY_TOPIC = "ENTRY_TOPIC";
    private static final String ENTRY_NAME = "ENTRY_NAME";
    private static final String ENTRY_CONTENT = "ENTRY_CONTENT";

    private static final String IS_EDIT_ENTRY = "IS_EDIT_ENTRY";
    private static final String IS_ENTRY_VIEW = "IS_ENTRY_VIEW";
    private static final String IS_ROOT_ENTRY = "IS_ROOT_ENTRY";
    private static final String IS_INDEX = "IS_INDEX";
    private static final String IS_CREATE_TOPIC = "IS_CREATE_TOPIC";
    private static final String IS_CREATE_ENTRY = "IS_CREATE_ENTRY";
    private static final String IS_TOPIC_LISTING = "IS_TOPIC_LISTING";
    private static final String IS_QUICK_LINK = "IS_QUICK_LINK";
    private static final String INITIAL_LOGIN = "INITIAL_LOGIN";
    private static final String LOGGING_OUT = "LOGGING_OUT";

    private final Map<String, Object> context;

    private final ContentProvider contentProvider;

    public MustacheBlogContext(ContentProvider contentProvider) {
        this.contentProvider = contentProvider;

        this.context = new HashMap<>();
        EntryDescriptor requestedEntry = contentProvider.getError();
        context.put(CONTENT, contentProvider.getEntryContentAsHttp(requestedEntry));
        context.put(TITLE, requestedEntry.getName());
        context.put(BLOG_CATEGORIES, getBlogCategories());
        context.put(QUICK_LINKS, getQuickLinks());
        context.put(IS_ROOT_ENTRY, Boolean.FALSE);
    }

    @Override 
    public void addBlogEntryContentToContext(String[] pathParts) {
        String categoryName = pathParts[1];
        String entryName = pathParts[2];
        Category requestedCategory = new Category(categoryName);
        EntryDescriptor requestedEntry = new EntryDescriptor(entryName, requestedCategory);
        context.put(CONTENT, contentProvider.getEntryContentAsHttp(requestedEntry));
        context.put(TITLE, requestedEntry.getName());
        for (EntryDescriptor existing : contentProvider.getEntries(requestedCategory)) {
            if (existing.getName().equals(entryName)) {
                context.put(IS_ENTRY_VIEW, Boolean.TRUE);
                context.put(ENTRY_TOPIC, categoryName);
                context.put(ENTRY_NAME, entryName);
                break;
            }
        }
    }

    @Override
    public void addIndexToContext() {
        EntryDescriptor indexEntry = contentProvider.getIndex();
        context.put(CONTENT, contentProvider.getEntryContentAsHttp(indexEntry));
        context.put(TITLE, indexEntry.getName());
        context.put(IS_INDEX, true);
    }

    @Override
    public void setRequestedSingleEntry(String entryName) {
        if (contentProvider.isQuickLink(entryName)) {
            EntryDescriptor requestedEntry = new EntryDescriptor(entryName, null);
            context.put(CONTENT, contentProvider.getEntryContentAsHttp(requestedEntry));
            context.put(TITLE, requestedEntry.getName());
            context.put(IS_QUICK_LINK, Boolean.TRUE);
            context.put(IS_ENTRY_VIEW, Boolean.TRUE);
            context.put(IS_ROOT_ENTRY, Boolean.TRUE);
            context.put(ENTRY_NAME, entryName);
        } else if (contentProvider.isBlogCategory(entryName)) {
            Category requestedCategory = new Category(entryName);
            context.put(BLOG_ENTRIES, getBlogEntries(requestedCategory));
            context.put(TITLE, requestedCategory.getName());
            context.put(IS_TOPIC_LISTING, Boolean.TRUE);
            context.put("category", entryName);
            context.remove(CONTENT);
        }
    }

    private List<Map<String, Object>> getQuickLinks() {
        EntryDescriptor[] entries = contentProvider.getQuickLinks();
        List<Map<String, Object>> categoryNames = new ArrayList<>();
        for (EntryDescriptor entry : entries) {
            Map<String, Object> categoryMap = new HashMap<>();
            categoryMap.put("name", entry.getName());
            categoryNames.add(categoryMap);
        }
        return categoryNames;
    }

    private List<Map<String, Object>> getBlogCategories() {
        Category[] categories = contentProvider.getCategories();
        List<Map<String, Object>> categoryNames = new ArrayList<>();
        for (Category category : categories) {
            Map<String, Object> categoryMap = new HashMap<>();
            categoryMap.put("name", category.getName());
            categoryNames.add(categoryMap);
        }
        return categoryNames;
    }

    private List<Map<String, Object>> getBlogEntries(Category category) {
        EntryDescriptor[] entries = contentProvider.getEntries(category);
        List<Map<String, Object>> entryNames = new ArrayList<>();
        for (EntryDescriptor entry : entries) {
            Map<String, Object> entryMap = new HashMap<>();
            entryMap.put("category", category.getName());
            entryMap.put("name", entry.getName());
            entryNames.add(entryMap);
        }
        return entryNames;
    }

    public Map<String, Object> getContextData() {
        return context;
    }

    @Override
    public void setIsLogin() {
        context.put(LOGIN, Boolean.TRUE);
        context.put(CONTENT, "");
        context.put(TITLE, "Login");
    }

    @Override 
    public void setIsUserDetails() {
        context.put(USER_DETAILS, Boolean.TRUE);
        context.put(CONTENT, "");
        context.put(TITLE, "User Details");
    }

    @Override 
    public void setUserDetails(User user) {
        context.put(USER_USERNAME, user.getUsername());
        context.put(USER_NAME, user.getName());
    }

    @Override
    public void setTemplateProperties(Properties properties) {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (String key : new TreeSet<>(properties.stringPropertyNames())) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("key", key);
            entry.put("value", properties.getProperty(key));
            entries.add(entry);
        }
        context.put(TEMPLATE_PROPERTIES, entries);
        context.put(HAS_TEMPLATE_PROPERTIES, !entries.isEmpty());
    }

    @Override
    public void setCsrfToken(String csrfToken) {
        context.put(CSRF_TOKEN, csrfToken);
    }

    @Override
    public void setUsername(String username) {
        context.put(USERNAME, username);
    }

    @Override
    public void setLoggedIn(boolean loginSuccessful) {
        context.put(LOGGED_IN, loginSuccessful);
    }

    @Override
    public void setName(String name) {
        context.put(NAME, name);
    }

    @Override
    public boolean isLoggedIn() {
        return context.containsKey(USERNAME);
    }

    @Override
    public String getUsername() {
        return (String) context.get(USERNAME);
    }

    @Override
    public String getName() {
        return (String) context.get(NAME);
    }

    @Override
    public void setInitialLogin() {
        context.put(INITIAL_LOGIN, Boolean.TRUE);
    }

    @Override
    public boolean isInitialLogin() {
        return context.containsKey(INITIAL_LOGIN) && (Boolean) context.get(INITIAL_LOGIN);
    }

    @Override
    public void setLoggingOut() {
        context.put(LOGGING_OUT, Boolean.TRUE);
    }

    @Override
    public boolean isLoggingOut() {
        return context.containsKey(LOGGING_OUT) && (Boolean) context.get(LOGGING_OUT);
    }

    @Override
    public void addSuccessMessage(String message) {
        context.put(SUCCESS_MESSAGE, message);
        context.put(HAS_SUCCESS_MESSAGE, Boolean.TRUE);
    }
    
    @Override
    public void addInfoMessage(String message) {
        context.put(INFO_MESSAGE, message);
        context.put(HAS_INFO_MESSAGE, Boolean.TRUE);
    }

    @Override
    public void addErrorMessage(String message) {
        context.put(ERROR_MESSAGE, message);
        context.put(HAS_ERROR_MESSAGE, Boolean.TRUE);
    }

    @Override
    public void setIsCreateTopic() {
        context.put(IS_CREATE_TOPIC, Boolean.TRUE);
        context.put(CONTENT, "");
        context.put(TITLE, "Create topic");
    }

    @Override
    public boolean isCreateTopic() {
        return context.containsKey(IS_CREATE_TOPIC) && (Boolean) context.get(IS_CREATE_TOPIC);
    }

    @Override
    public void setIsCreateEntry() {
        context.put(IS_CREATE_ENTRY, Boolean.TRUE);
        context.put(CONTENT, "");
        context.put(TITLE, "Create entry");
    }

    @Override
    public void setIsEditEntry() {
        context.put(IS_EDIT_ENTRY, Boolean.TRUE);
        context.put(CONTENT, "");
        context.put(TITLE, "Edit entry");
    }

    @Override
    public void setEntryTopic(String topic) {
        if (topic == null || topic.isEmpty()) {
            context.remove(ENTRY_TOPIC);
            context.put(IS_ROOT_ENTRY, Boolean.TRUE);
        } else {
            context.put(ENTRY_TOPIC, topic);
            context.put(IS_ROOT_ENTRY, Boolean.FALSE);
        }
    }

    @Override
    public void setEntryDraft(String name, String content) {
        context.put(ENTRY_NAME, name);
        context.put(ENTRY_CONTENT, content);
    }

    @Override
    public boolean isCreateEntry() {
        return context.containsKey(IS_CREATE_ENTRY) && (Boolean) context.get(IS_CREATE_ENTRY);
    }
}
