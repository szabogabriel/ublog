package info.gabrielszabo.ublog.content;

import info.gabrielszabo.ublog.domain.Category;
import info.gabrielszabo.ublog.domain.EntryDescriptor;

public interface ContentProvider {
    
    Category[] getCategories();

    EntryDescriptor[] getEntries(Category category);

    EntryDescriptor[] getQuickLinks();

    String getEntryContentAsHttp(EntryDescriptor entry);

    EntryDescriptor getIndex();

    EntryDescriptor getError();

    boolean isQuickLink(String value);

    boolean isBlogCategory(String value);

}
