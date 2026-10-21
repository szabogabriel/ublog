package info.gabrielszabo.ublog.service.requestHandlers;

import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.template.TemplateService;

public class ContentHandler extends AbstractRequestHandler {

    public ContentHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService) {
        super(contextFactory, sessionService, templateService);
    }

    @Override
    public boolean matches(String path) {
        return true;
    }

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        String[] pathParts = request.getPath().split("/");
        if (session.isLoggedIn()) {
            context.setCsrfToken(sessionService.getCsrfToken(session.getSessionId()));
        }
        if (pathParts.length == 2) {
            context.setRequestedSingleEntry(pathParts[1]);
        } else if (pathParts.length == 3) {
            context.addBlogEntryContentToContext(pathParts);
        }

        return createHttpResponseFromContext(context, session);
    }
}