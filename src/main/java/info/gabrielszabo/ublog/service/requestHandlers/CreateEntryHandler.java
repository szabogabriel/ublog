package info.gabrielszabo.ublog.service.requestHandlers;

import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.server.http.HttpMethods;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.server.http.HttpStatus;
import info.gabrielszabo.ublog.server.http.MultipartForm;
import info.gabrielszabo.ublog.service.ContentService;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.template.TemplateService;

public class CreateEntryHandler extends AbstractRequestHandler {

    private static final String PATH_PREFIX = "/createentry/";

    private final ContentService contentService;

    public CreateEntryHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService, ContentService contentService) {
        super(contextFactory, sessionService, templateService);
        this.contentService = contentService;
    }

    @Override
    public boolean matches(String path) {
        String lower = path.toLowerCase();
        return lower.startsWith(PATH_PREFIX) || lower.equals("/createentry");
    }

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        if (!session.isLoggedIn()) {
            return createClientError(HttpStatus.FORBIDDEN, "User not logged in");
        }

        String topic = request.getPath().length() > PATH_PREFIX.length()
                ? request.getPath().substring(PATH_PREFIX.length()) : "";
        if (!ContentService.isRoot(topic) && !contentService.topicExists(topic)) {
            return createClientError(HttpStatus.NOT_FOUND, "Unknown topic");
        }

        context.setIsCreateEntry();
        context.setEntryTopic(topic);
        context.setCsrfToken(sessionService.getCsrfToken(session.getSessionId()));

        if (request.getMethod() == HttpMethods.GET) {
            return createHttpResponseFromContext(context, session);
        }
        if (request.getMethod() != HttpMethods.POST) {
            HttpResponse response = createClientError(HttpStatus.METHOD_NOT_ALLOWED, "Create entry requires GET or POST");
            response.addHeader("Allow", "GET, POST");
            return response;
        }
        if (!MultipartForm.isMultipart(request.getContentType())) {
            return createClientError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Expected multipart/form-data");
        }

        MultipartForm form;
        try {
            form = MultipartForm.parse(request.getContentType(), request.getBodyBytes());
        } catch (IllegalArgumentException e) {
            return createClientError(HttpStatus.BAD_REQUEST, "Malformed multipart request");
        }

        if (!sessionService.isCsrfTokenValid(session.getSessionId(), form.get("csrfToken"))) {
            return createClientError(HttpStatus.FORBIDDEN, "Invalid CSRF token");
        }

        String entryName = form.get("entryName") == null ? "" : form.get("entryName").trim();
        String content = form.get("content") == null ? "" : form.get("content");
        if (entryName.isEmpty() || content.isBlank()) {
            context.setEntryDraft(entryName, content);
            context.addErrorMessage("Please provide an entry name and some content.");
            return createHttpResponseFromContext(context, session);
        }

        if (contentService.createEntry(topic, entryName, content, null)) {
            context.addSuccessMessage("Successfully created entry " + entryName);
        } else {
            context.setEntryDraft(entryName, content);
            context.addErrorMessage("Failed to create entry " + entryName
                    + ". Use letters, digits, spaces, '-' or '_' and choose a name that is not taken.");
        }
        return createHttpResponseFromContext(context, session);
    }

}
