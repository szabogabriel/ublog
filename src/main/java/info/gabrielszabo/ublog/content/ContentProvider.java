package info.gabrielszabo.ublog.content;

import info.gabrielszabo.ublog.domain.Category;
import info.gabrielszabo.ublog.domain.EntryDescriptor;

public interface ContentProvider {
    
    Category[] getCategories();

    EntryDescriptor[] getEntries(Category category);

    EntryDescriptor[] getEntries(Category category, boolean includeUnlisted);

    EntryDescriptor[] getQuickLinks();

    String getEntryContentAsHttp(EntryDescriptor entry);

    String getEntryContentAsHttp(EntryDescriptor entry, boolean includeUnlisted);

    /** Renders a randomly named unlisted file; category null means the blog root. Returns null if not found. */
    String getUnlistedContentAsHttp(Category category, String fileName);

    /** The random file name behind an unlisted entry, or null if the entry is not a valid unlisted link. */
    String getUnlistedFileName(Category category, String entryName);

    EntryDescriptor getIndex();

    EntryDescriptor getError();

    boolean isQuickLink(String value);

    boolean isBlogCategory(String value);

}
