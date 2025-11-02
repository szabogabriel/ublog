package info.gabrielszabo.ublog.service.mustache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import info.gabrielszabo.ublog.content.ContentProvider;
import info.gabrielszabo.ublog.domain.Category;
import info.gabrielszabo.ublog.domain.EntryDescriptor;
import info.gabrielszabo.ublog.service.BlogContext;

public class MustacheBlogContext implements BlogContext{
        private static final String CONTENT = "DATA";
    private static final String TITLE = "PAGE_TITLE";
    private static final String BLOG_ENTRIES = "ENTRIES";
    private static final String BLOG_CATEGORIES = "TOPICS";
    private static final String QUICK_LINKS = "QUICK_LINKS";

    private Map<String, Object> context;

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

    public void addBlogEntryContentToContext(String[] pathParts) {
        String categoryName = pathParts[1];
        String entryName = pathParts[2];
        Category requestedCategory = new Category(categoryName);
        EntryDescriptor requestedEntry = new EntryDescriptor(entryName, requestedCategory);
        context.put(CONTENT, contentProvider.getEntryContentAsHttp(requestedEntry));
        context.put(TITLE, requestedEntry.getName());
    }

    public void addIndexToContext() {
        EntryDescriptor indexEntry = contentProvider.getIndex();
        context.put(CONTENT, contentProvider.getEntryContentAsHttp(indexEntry));
        context.put(TITLE, indexEntry.getName());
    }

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
}
