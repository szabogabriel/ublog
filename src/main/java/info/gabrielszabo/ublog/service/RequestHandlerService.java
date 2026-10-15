package info.gabrielszabo.ublog.service;

import java.util.List;

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
        BlogContext blogContext = contextFactory.createContext();
        SessionContext sessionContext = new SessionContext();

        String requestedPath = request.getPath();

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

            String body = request.getBody();

            String username = getUsernameFromLoginData(body);
            String password = getPasswordFromLoginData(body);

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

        if (isLoggedIn) {
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
        if (session.isLoggedIn()) {
            if (session.isInitialLogin()) {
                response.addCookie(createSessionCookie(session.getSessionId()));
            } else if (context.isLoggingOut()) {
                sessionService.invalidateSession(session.getSessionId());
                response.addCookie(createSessionInvalidationCookie());
            } else {
                sessionService.refreshSession(session.getSessionId());
            }
        }

        return response;
    }

    private Cookie createSessionCookie(String sessionId) {
        Cookie ret = new Cookie("session", sessionId + "");
        return ret;
    }

    private Cookie createSessionInvalidationCookie() {
        Cookie ret = new Cookie("session", "", 0);
        return ret;
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

    private String getUsernameFromLoginData(String loginData) {
        String[] parts = loginData.split("&");
        for (String part : parts) {
            String[] keyValue = part.split("=");
            if (keyValue.length == 2 && keyValue[0].equals("username")) {
                return keyValue[1];
            }
        }
        return null;
    }

    private String getPasswordFromLoginData(String loginData) {
        String[] parts = loginData.split("&");
        for (String part : parts) {
            String[] keyValue = part.split("=");
            if (keyValue.length == 2 && keyValue[0].equals("password")) {
                return keyValue[1];
            }
        }
        return null;
    }

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
