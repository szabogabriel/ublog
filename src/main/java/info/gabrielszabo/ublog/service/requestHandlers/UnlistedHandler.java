package info.gabrielszabo.ublog.service.requestHandlers;

import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.server.http.HttpMethods;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.server.http.HttpStatus;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.template.TemplateService;

public class UnlistedHandler extends AbstractRequestHandler {

    private static final String PATH_PREFIX = "/unlisted/";

    public UnlistedHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService) {
        super(contextFactory, sessionService, templateService);
    }

    @Override
    public boolean matches(String path) {
        return path.toLowerCase().startsWith(PATH_PREFIX);
    }

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        if (request.getMethod() != HttpMethods.GET) {
            HttpResponse response = createClientError(HttpStatus.METHOD_NOT_ALLOWED, "Unlisted entries require GET");
            response.addHeader("Allow", "GET");
            return response;
        }

        // /unlisted/{file} is a root entry, /unlisted/{topic}/{file} a topic entry.
        String[] parts = request.getPath().substring(PATH_PREFIX.length()).split("/", -1);
        if (parts.length != 1 && parts.length != 2) {
            return createClientError(HttpStatus.NOT_FOUND, "Unknown entry");
        }
        String topic = parts.length == 2 ? parts[0] : "";
        String fileName = parts[parts.length - 1];

        if (!context.addUnlistedEntryToContext(topic, fileName)) {
            return createClientError(HttpStatus.NOT_FOUND, "Unknown entry");
        }
        if (session.isLoggedIn()) {
            context.setCsrfToken(sessionService.getCsrfToken(session.getSessionId()));
        }
        return createHttpResponseFromContext(context, session);
    }
    
}
