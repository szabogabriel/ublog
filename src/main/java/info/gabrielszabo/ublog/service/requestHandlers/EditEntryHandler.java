package info.gabrielszabo.ublog.service.requestHandlers;

import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.server.http.HttpMethods;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.server.http.MultipartForm;
import info.gabrielszabo.ublog.service.ContentService;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.template.TemplateService;

public class EditEntryHandler extends AbstractRequestHandler {

    private static final String PATH_PREFIX = "/editentry/";

    private final ContentService contentService;

    public EditEntryHandler(BlogContextFactory contextFactory, SessionService sessionService,
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
            return createClientError(403, "User not logged in");
        }

        String[] parts = request.getPath().substring(PATH_PREFIX.length()).split("/", -1);
        if (parts.length != 2) {
            return createClientError(404, "Unknown entry");
        }
        String topic = parts[0];
        String entry = parts[1];
        String currentContent = contentService.getEntryContent(topic, entry);
        if (currentContent == null) {
            return createClientError(404, "Unknown entry");
        }

        context.setIsEditEntry();
        context.setEntryTopic(topic);
        context.setEntryDraft(entry, currentContent);
        context.setCsrfToken(sessionService.getCsrfToken(session.getSessionId()));

        HttpMethods method = request.getMethod();
        if (method == HttpMethods.GET) {
            return createHttpResponseFromContext(context, session);
        }
        // POST is accepted because plain HTML forms cannot send PUT.
        if (method != HttpMethods.PUT && method != HttpMethods.POST) {
            HttpResponse response = createClientError(405, "Edit entry requires GET or PUT");
            response.addHeader("Allow", "GET, PUT, POST");
            return response;
        }
        if (!MultipartForm.isMultipart(request.getContentType())) {
            return createClientError(415, "Expected multipart/form-data");
        }

        MultipartForm form;
        try {
            form = MultipartForm.parse(request.getContentType(), request.getBody());
        } catch (IllegalArgumentException e) {
            return createClientError(400, "Malformed multipart request");
        }

        if (!sessionService.isCsrfTokenValid(session.getSessionId(), form.get("csrfToken"))) {
            return createClientError(403, "Invalid CSRF token");
        }

        String content = form.get("content") == null ? "" : form.get("content");
        context.setEntryDraft(entry, content);
        if (content.isBlank()) {
            context.addErrorMessage("The entry content must not be empty.");
        } else if (contentService.updateEntry(topic, entry, content)) {
            context.addSuccessMessage("Successfully updated entry " + entry);
        } else {
            context.addErrorMessage("Failed to update entry " + entry);
        }
        return createHttpResponseFromContext(context, session);
    }

}
