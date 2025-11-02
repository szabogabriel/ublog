package info.gabrielszabo.ublog.service;

public interface BlogContext {

    void addBlogEntryContentToContext(String[] pathParts);
    void addIndexToContext();
    void setRequestedSingleEntry(String entryName);
    
}
