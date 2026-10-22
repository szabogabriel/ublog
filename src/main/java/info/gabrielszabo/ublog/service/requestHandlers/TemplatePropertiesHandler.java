package info.gabrielszabo.ublog.service.requestHandlers;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.domain.User;
import info.gabrielszabo.ublog.server.http.HttpMethods;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.server.http.HttpStatus;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.UserService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.template.TemplateService;

public class TemplatePropertiesHandler extends AbstractRequestHandler {

    private static final String FIELD_PREFIX = "prop_";

    private final UserService userService;

    public TemplatePropertiesHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService, UserService userService) {
        super(contextFactory, sessionService, templateService);
        this.userService = userService;
    }

    @Override
    public boolean matches(String path) {
        return path.equals("/templateproperties");
    }

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        if (!session.isLoggedIn()) {
            return createClientError(HttpStatus.FORBIDDEN, "User not logged in");
        }
        if (request.getMethod() != HttpMethods.POST) {
            HttpResponse response = createClientError(HttpStatus.METHOD_NOT_ALLOWED,
                    "Template properties update requires POST");
            response.addHeader("Allow", "POST");
            return response;
        }
        if (!isFormContentType(request.getContentType())) {
            return createClientError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Expected application/x-www-form-urlencoded");
        }

        Map<String, String> fields;
        try {
            fields = parseForm(request.getBody());
        } catch (IllegalArgumentException e) {
            return createUserDetailsResponse(context, session, false, "The submitted form was malformed.");
        }

        if (!sessionService.isCsrfTokenValid(session.getSessionId(), fields.get("csrfToken"))) {
            return createClientError(HttpStatus.FORBIDDEN, "Invalid CSRF token");
        }

        // Only keys that already exist in the template's properties can be updated.
        Properties properties = getTemplateProperties();
        for (String key : properties.stringPropertyNames()) {
            String submitted = fields.get(FIELD_PREFIX + key);
            if (submitted != null) {
                properties.setProperty(key, submitted);
            }
        }

        try {
            saveTemplateProperties(properties);
        } catch (IllegalStateException e) {
            return createUserDetailsResponse(context, session, false,
                    "The template properties could not be saved. Please try again.");
        }

        return createUserDetailsResponse(context, session, true, "Template properties have been updated.");
    }

    private HttpResponse createUserDetailsResponse(BlogContext context, SessionContext session, boolean success,
            String message) {
        Optional<User> userOpt = userService.getUser(context.getUsername());
        if (userOpt.isEmpty()) {
            return createClientError(HttpStatus.NOT_FOUND, "User not found");
        }

        context.setIsUserDetails();
        context.setCsrfToken(sessionService.getCsrfToken(session.getSessionId()));
        context.setUserDetails(userOpt.get());
        context.setTemplateProperties(getTemplateProperties());

        if (success) {
            context.addSuccessMessage(message);
        } else {
            context.addErrorMessage(message);
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

    private Map<String, String> parseForm(String body) {
        if (body == null || body.isEmpty()) {
            throw new IllegalArgumentException("Missing form fields");
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
        return fields;
    }
}
