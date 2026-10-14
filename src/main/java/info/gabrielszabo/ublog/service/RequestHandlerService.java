package info.gabrielszabo.ublog.service;

import java.util.List;

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
        BlogContext context = contextFactory.createContext();

        String requestedPath = request.getPath();

        if (isIndex(requestedPath)) {
            context.addIndexToContext();
            context.setLoggedIn(isLoggedIn(request));
            context.setUsername(getSessionUsername(request));
        } else if (isLogin(requestedPath)) {
            context.setIsLogin();
        } else if (isLoginAction(requestedPath)) {
            
            String body = request.getBody();

            String username = getUsernameFromLoginData(body);
            String password = getPasswordFromLoginData(body);

            log.logTrace("Login action: " + username);

            boolean loginSuccessful = loginService.login(username, password);
            if (loginSuccessful) {
                log.logInfo("Login successful for user: " + username);
                context.setInitialLogin();
                context.setName(userService.getFullName(username));
                context.setUsername(username);
                sessionService.createSession(username);
            } else {
                log.logInfo("Login failed for user: " + username);
            }

            context.setLoggedIn(loginSuccessful);
            context.addIndexToContext();
        } else if (isLogout(requestedPath)) {
            context.setLoggingOut();
            context.setUsername(getSessionUsername(request));
            context.addIndexToContext();
        } else {
            context.setLoggedIn(isLoggedIn(request));
            context.setUsername(getSessionUsername(request));
            String[] pathParts = requestedPath.split("/");
            if (pathParts.length == 2) {
                String entryName = pathParts[1];
                context.setRequestedSingleEntry(entryName);
            } else if (pathParts.length == 3) {
                // Should render entry
                context.addBlogEntryContentToContext(pathParts);
            }
        }

        return createHttpResponseFromContext(context);
    }

    private HttpResponse createHttpResponseFromContext(BlogContext context) {
        HttpResponse response = new HttpResponse();

        response.setStatusCode(200);
        response.setContentType("text/html");
        response.setBody(templateService.render(PAGE_TEMPLATE, context));

        //Session handling
        if (context.isLoggedIn()) {
            if (context.isInitialLogin()) {
                response.addCookie(sessionService.getSessionCookie(context.getUsername()));
            } else if (context.isLoggingOut()) {
                sessionService.invalidateSession(context.getUsername());
                response.addCookie(sessionService.getSessionInvalidationCookie(context.getUsername()));
            } else {
                sessionService.refreshSession(context.getUsername());
            }
        }

        return response;
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

    private String getSessionUsername(HttpRequest request) {
        List<Cookie> cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("session")) {
                System.out.println("XXX Found session cookie: " + cookie);
                return cookie.getValue();
            }
        }
        System.out.println("XXX No session cookie found");
        return null;
    }

}
