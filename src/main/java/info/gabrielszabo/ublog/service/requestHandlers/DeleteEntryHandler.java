package info.gabrielszabo.ublog.service.requestHandlers;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.server.http.HttpMethods;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.server.http.HttpStatus;
import info.gabrielszabo.ublog.service.ContentService;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.template.TemplateService;

public class DeleteEntryHandler extends AbstractRequestHandler {

    private static final String PATH_PREFIX = "/deleteentry/";

    private final ContentService contentService;

    public DeleteEntryHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService, ContentService contentService) {
        super(contextFactory, sessionService, templateService);
        this.contentService = contentService;
    }

    @Override
    public boolean matches(String path) {
        return path.toLowerCase().startsWith(PATH_PREFIX);
    }

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        if (!session.isLoggedIn()) {
            return createClientError(HttpStatus.FORBIDDEN, "User not logged in");
        }
        if (request.getMethod() != HttpMethods.POST) {
            HttpResponse response = createClientError(HttpStatus.METHOD_NOT_ALLOWED, "Delete entry requires POST");
            response.addHeader("Allow", "POST");
            return response;
        }

        String[] parts = request.getPath().substring(PATH_PREFIX.length()).split("/", -1);
        if (parts.length != 1 && parts.length != 2) {
            return createClientError(HttpStatus.NOT_FOUND, "Unknown entry");
        }
        // A single segment is an entry in the blog root.
        String topic = parts.length == 2 ? parts[0] : "";
        String entry = parts[parts.length - 1];

        if (!sessionService.isCsrfTokenValid(session.getSessionId(), parseCsrfToken(request.getBody()))) {
            return createClientError(HttpStatus.FORBIDDEN, "Invalid CSRF token");
        }
        if (!contentService.deleteEntry(topic, entry)) {
            return createClientError(HttpStatus.NOT_FOUND, "Unknown entry");
        }

        HttpResponse response = new HttpResponse();
        response.setStatus(HttpStatus.SEE_OTHER);
        response.addHeader("Location", topic.isEmpty() ? "/" : "/" + topic);
        response.setContentType("text/plain; charset=UTF-8");
        response.setBody("Entry deleted");
        return response;
    }

    private String parseCsrfToken(String body) {
        if (body == null) {
            return null;
        }
        for (String pair : body.split("&")) {
            int separator = pair.indexOf('=');
            if (separator > 0 && URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8)
                    .equals("csrfToken")) {
                return URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
