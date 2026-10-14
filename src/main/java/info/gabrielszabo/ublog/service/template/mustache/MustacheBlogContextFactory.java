package info.gabrielszabo.ublog.service.template.mustache;

import info.gabrielszabo.ublog.content.ContentProvider;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;

public class MustacheBlogContextFactory implements BlogContextFactory {

    private final ContentProvider contentProvider;

    public MustacheBlogContextFactory(ContentProvider contentProvider) {
        this.contentProvider = contentProvider;
    }

    @Override
    public BlogContext createContext() {
        return new MustacheBlogContext(contentProvider);
    }
    
}
