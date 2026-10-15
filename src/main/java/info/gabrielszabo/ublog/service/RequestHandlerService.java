package info.gabrielszabo.ublog.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import info.gabrielszabo.ublog.config.Config;
import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.server.http.Cookie;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpRequestHandler;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.log.LogService;
import info.gabrielszabo.ublog.service.template.TemplateService;

public class RequestHandlerService implements HttpRequestHandler {
    private static final String PAGE_TEMPLATE = "index.template";

    private final BlogContextFactory contextFactory;
    private final TemplateService templateService;
    private final UserService userService;
    private final LoginService loginService;
    private final SessionService sessionService;
    private final LogService log;

    public RequestHandlerService(BlogContextFactory contextFactory, TemplateService templateService,
            UserService userService, LoginService loginService, SessionService sessionService, LogService log) {
        this.contextFactory = contextFactory;
        this.templateService = templateService;
        this.userService = userService;
        this.loginService = loginService;
        this.sessionService = sessionService;
        this.log = log;
    }

    @Override
    public HttpResponse handleRequest(HttpRequest request) {
        log.logInfo("Handling request " + request.getPath());
        String requestedPath = request.getPath();
        LoginForm loginForm = null;
        if (isLoginAction(requestedPath)) {
            if (request.getMethod() != info.gabrielszabo.ublog.server.http.HttpMethods.POST) {
                HttpResponse response = createClientError(405, "Login requires POST");
                response.addHeader("Allow", "POST");
                return response;
            }
            if (!isFormContentType(request.getContentType())) {
                return createClientError(415, "Expected application/x-www-form-urlencoded");
            }
            try {
                loginForm = parseLoginForm(request.getBody());
            } catch (IllegalArgumentException e) {
                return createClientError(400, "Invalid login form");
            }
        }
        if (isLogout(requestedPath) && request.getMethod() != info.gabrielszabo.ublog.server.http.HttpMethods.POST) {
            HttpResponse response = createClientError(405, "Logout requires POST");
            response.addHeader("Allow", "POST");
            return response;
        }

        BlogContext blogContext = contextFactory.createContext();
        SessionContext sessionContext = new SessionContext();

        String sessionId = getSessionId(request);
        String sessionUsername = sessionService.getUsernameForSession(sessionId);
        boolean isLoggedIn = sessionUsername != null;

        sessionContext.setLoggedIn(isLoggedIn);
        sessionContext.setSessionId(sessionId);
        blogContext.setLoggedIn(isLoggedIn);

        if (isIndex(requestedPath)) {
            blogContext.setUsername(sessionUsername);
            blogContext.addIndexToContext();
        } else if (isLogin(requestedPath)) {
            blogContext.setIsLogin();
        } else if (isLoginAction(requestedPath)) {

            String username = loginForm.username();
            String password = loginForm.password();

            log.logTrace("Login action: " + username);

            boolean loginSuccessful = loginService.login(username, password);
            if (loginSuccessful) {
                log.logInfo("Login successful for user: " + username);
                sessionContext.setInitialLogin(true);
                blogContext.setName(userService.getFullName(username));
                blogContext.setUsername(username);
                String newSessionId = sessionService.createSession(username);
                sessionContext.setSessionId(newSessionId);
                sessionContext.setLoggedIn(true);
            } else {
                log.logInfo("Login failed for user: " + username);
            }

            blogContext.setLoggedIn(loginSuccessful);
            blogContext.addIndexToContext();
        } else if (isLogout(requestedPath)) {
            sessionService.invalidateSession(sessionId);
            sessionContext.setLoggingOut(true);
            blogContext.setLoggingOut();
            sessionContext.setLoggedIn(false);
            blogContext.setLoggedIn(false);
            blogContext.setUsername(getSessionUsername(request));
            blogContext.addIndexToContext();
        } else {
            blogContext.setUsername(getSessionUsername(request));
            String[] pathParts = requestedPath.split("/");
            if (pathParts.length == 2) {
                String entryName = pathParts[1];
                blogContext.setRequestedSingleEntry(entryName);
            } else if (pathParts.length == 3) {
                // Should render entry
                blogContext.addBlogEntryContentToContext(pathParts);
            }
        }

        if (isLoggedIn && !isLogout(requestedPath)) {
            sessionService.refreshSession(sessionId);
        }

        return createHttpResponseFromContext(blogContext, sessionContext);
    }

    private HttpResponse createHttpResponseFromContext(BlogContext context, SessionContext session) {
        HttpResponse response = new HttpResponse();

        response.setStatusCode(200);
        response.setContentType("text/html");
        response.setBody(templateService.render(PAGE_TEMPLATE, context));

        // Session handling
        if (session.isLoggingOut()) {
            response.addCookie(createSessionInvalidationCookie());
        } else if (session.isInitialLogin()) {
            response.addCookie(createSessionCookie(session.getSessionId()));
        }

        return response;
    }

    private Cookie createSessionCookie(String sessionId) {
        return configureSessionCookie(new Cookie("session", sessionId));
    }

    private Cookie createSessionInvalidationCookie() {
        return configureSessionCookie(new Cookie("session", "", 0));
    }

    private Cookie configureSessionCookie(Cookie cookie) {
        return cookie.setPath("/").setHttpOnly(true).setSecure(Boolean.parseBoolean(Config.SESSION_COOKIE_SECURE.value()))
            .setSameSite("Lax");
    }

    private boolean isIndex(String requestPath) {
        return requestPath.equals("") || requestPath.equals("/") || requestPath.equals("/index");
    }

    private boolean isLogin(String requestPath) {
        return requestPath.equals("/login");
    }

    private boolean isLoginAction(String requestPath) {
        return requestPath.equals("/loginaction");
    }

    private boolean isLogout(String requestPath) {
        return requestPath.equals("/logout");
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

    private HttpResponse createClientError(int statusCode, String message) {
        HttpResponse response = new HttpResponse();
        response.setStatusCode(statusCode);
        response.setContentType("text/plain; charset=UTF-8");
        response.setBody(message);
        return response;
    }

    private record LoginForm(String username, String password) {}

    private boolean isLoggedIn(HttpRequest request) {
        List<Cookie> cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("session")) {
                return true;
            }
        }
        return false;
    }

    private String getSessionId(HttpRequest request) {
        List<Cookie> cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("session")) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String getSessionUsername(HttpRequest request) {
        String sessionId = getSessionId(request);
        if (sessionId == null) {
            return null;
        }
        return sessionService.getUsernameForSession(sessionId);
    }

}
