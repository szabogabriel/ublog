package info.gabrielszabo.ublog.service.requestHandlers;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.server.http.Cookie;
import info.gabrielszabo.ublog.server.http.HttpMethods;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.service.LoginService;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.UserService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.log.LogService;
import info.gabrielszabo.ublog.service.template.TemplateService;

public class LoginActionHandler extends AbstractRequestHandler {

    private final UserService userService;
    private final LoginService loginService;
    private final LogService log;

    public LoginActionHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService, UserService userService, LoginService loginService, LogService log) {
        super(contextFactory, sessionService, templateService);
        this.userService = userService;
        this.loginService = loginService;
        this.log = log;
    }

    @Override
    public boolean matches(String path) {
        return path.equals("/loginaction");
    }

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        if (request.getMethod() != HttpMethods.POST) {
            HttpResponse response = createClientError(405, "Login requires POST");
            response.addHeader("Allow", "POST");
            return response;
        }
        if (!isFormContentType(request.getContentType())) {
            return createClientError(415, "Expected application/x-www-form-urlencoded");
        }

        LoginForm loginForm;
        try {
            loginForm = parseLoginForm(request.getBody());
        } catch (IllegalArgumentException e) {
            return createClientError(400, "Invalid login form");
        }

        String username = loginForm.username();
        String password = loginForm.password();

        log.logTrace("Login action: " + username);

        boolean loginSuccessful = loginService.login(username, password);
        if (loginSuccessful) {
            log.logInfo("Login successful for user: " + username);
            session.setInitialLogin(true);
            context.setName(userService.getFullName(username));
            context.setUsername(username);
            String newSessionId = sessionService.createSession(username);
            session.setSessionId(newSessionId);
            session.setLoggedIn(true);
        } else {
            log.logInfo("Login failed for user: " + username);
        }

        context.setLoggedIn(loginSuccessful);
        context.addIndexToContext();

        HttpResponse response = createHttpResponseFromContext(context, session);

        if (session.isInitialLogin()) {
            response.addCookie(createSessionCookie(session.getSessionId()));
        }
        
        return response;
    }

    private boolean isFormContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        int parameterStart = contentType.indexOf(';');
        String mediaType = parameterStart >= 0 ? contentType.substring(0, parameterStart) : contentType;
        return mediaType.trim().equalsIgnoreCase("application/x-www-form-urlencoded");
    }

    private LoginForm parseLoginForm(String body) {
        if (body == null || body.isEmpty()) {
            throw new IllegalArgumentException("Missing login form fields");
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
        String username = fields.get("username");
        String password = fields.get("password");
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Missing login form field");
        }
        return new LoginForm(username, password);
    }

    private record LoginForm(String username, String password) {
    }

    private Cookie createSessionCookie(String sessionId) {
        return configureSessionCookie(new Cookie("session", sessionId));
    }

}