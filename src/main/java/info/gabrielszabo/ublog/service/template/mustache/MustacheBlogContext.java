package info.gabrielszabo.ublog.service.template.mustache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import info.gabrielszabo.ublog.content.ContentProvider;
import info.gabrielszabo.ublog.domain.Category;
import info.gabrielszabo.ublog.domain.EntryDescriptor;
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
    private static final String INITIAL_LOGIN = "INITIAL_LOGIN";
    private static final String LOGGING_OUT = "LOGGING_OUT";
    private static final String IS_INDEX = "IS_INDEX";

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
    }

    @Override 
    public void addBlogEntryContentToContext(String[] pathParts) {
        String categoryName = pathParts[1];
        String entryName = pathParts[2];
        Category requestedCategory = new Category(categoryName);
        EntryDescriptor requestedEntry = new EntryDescriptor(entryName, requestedCategory);
        context.put(CONTENT, contentProvider.getEntryContentAsHttp(requestedEntry));
        context.put(TITLE, requestedEntry.getName());
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
        } else if (contentProvider.isBlogCategory(entryName)) {
            Category requestedCategory = new Category(entryName);
            context.put(BLOG_ENTRIES, getBlogEntries(requestedCategory));
            context.put(TITLE, requestedCategory.getName());
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
}
