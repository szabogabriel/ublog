package info.gabrielszabo.ublog.service.requestHandlers;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.server.http.HttpMethods;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.server.http.HttpStatus;
import info.gabrielszabo.ublog.service.ContentService;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.UserService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.template.TemplateService;

public class CreateTopicHandler extends AbstractRequestHandler {

    private final ContentService contentService;

    public CreateTopicHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService, ContentService contentService, UserService userService) {
        super(contextFactory, sessionService, templateService, userService);
        this.contentService = contentService;
    }

    @Override
    public boolean matches(String path) {
        return path.equals("/createtopic");
    }

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        if (!session.isLoggedIn()) {
            return createClientError(HttpStatus.FORBIDDEN, "User not logged in");
        }

        if (!session.isEditor()) {
            return createClientError(HttpStatus.FORBIDDEN, "User not an editor");
        }

        context.setIsCreateTopic();
        context.setCsrfToken(sessionService.getCsrfToken(session.getSessionId()));

        if (request.getMethod() == HttpMethods.GET) {
            return createHttpResponseFromContext(context, session);
        }
        if (request.getMethod() != HttpMethods.POST) {
            HttpResponse response = createClientError(HttpStatus.METHOD_NOT_ALLOWED, "Create topic requires GET or POST");
            response.addHeader("Allow", "GET, POST");
            return response;
        }
        if (!isFormContentType(request.getContentType())) {
            return createClientError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Expected application/x-www-form-urlencoded");
        }

        CreateTopicForm createTopicForm;
        try {
            createTopicForm = parseCreateTopicForm(request.getBody());
        } catch (IllegalArgumentException e) {
            context.addErrorMessage("Please provide a topic name.");
            return createHttpResponseFromContext(context, session);
        }

        if (!sessionService.isCsrfTokenValid(session.getSessionId(), createTopicForm.csrfToken())) {
            return createClientError(HttpStatus.FORBIDDEN, "Invalid CSRF token");
        }

        boolean created = contentService.createTopic(createTopicForm.topicName());
        if (!created) {
            context.addErrorMessage("Failed to create topic with name " + createTopicForm.topicName());
        } else {
            context.addInfoMessage("Successfully created topic with name " + createTopicForm.topicName());
        }
        return createHttpResponseFromContext(context, session);
    }

    private boolean isFormContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        int parameterStart = contentType.indexOf(';');
        String mediaType = parameterStart >= 0 ? contentType.substring(0, parameterStart) : contentType;
        return mediaType.trim().equalsIgnoreCase("application/x-www-form-urlencoded");
    }

    private CreateTopicForm parseCreateTopicForm(String body) {
        if (body == null || body.isEmpty()) {
            throw new IllegalArgumentException("Missing create topic fields");
        }

        Map<String, String> fields = new LinkedHashMap<>();
        for (String pair : body.split("&", -1)) {
            int separator = pair.indexOf('=');
            if (separator < 0) {
                throw new IllegalArgumentException("Malformed form field");
            }
            String name = URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8);
            if (fields.putIfAbsent(name, value) != null) {
                throw new IllegalArgumentException("Duplicate form field");
            }
        }

        String topicName = fields.get("topicName");
        String csrfToken = fields.get("csrfToken");
        if (topicName == null || topicName.isBlank()) {
            throw new IllegalArgumentException("Missing topic name");
        }

        return new CreateTopicForm(topicName.trim(), csrfToken);
    }

    private record CreateTopicForm(String topicName, String csrfToken) {
    }
}
