package info.gabrielszabo.ublog.service.requestHandlers;

import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.template.TemplateService;
import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;

public class IndexHandler extends AbstractRequestHandler {

    public IndexHandler(BlogContextFactory contextFactory, SessionService sessionService, TemplateService templateService) {
        super(contextFactory, sessionService, templateService);
    }

    @Override
    public boolean matches(String path) {
        return path.equals("") || path.equals("/") || path.equals("/index");
    }

    @Override
    protected HttpResponse handleRequest(BlogContext blogContext, SessionContext sessionContext, HttpRequest request) {
        blogContext.addIndexToContext();

        return createHttpResponseFromContext(blogContext, sessionContext);
    }
}
