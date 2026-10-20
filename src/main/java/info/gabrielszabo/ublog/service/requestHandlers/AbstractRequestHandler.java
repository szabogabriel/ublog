package info.gabrielszabo.ublog.service.requestHandlers;

import java.util.List;

import info.gabrielszabo.ublog.config.Config;
import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.server.http.Cookie;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.server.http.HttpStatus;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.template.TemplateService;

public abstract class AbstractRequestHandler {

    private static final String PAGE_TEMPLATE = "index.template";

    protected final BlogContextFactory contextFactory;
    protected final SessionService sessionService;
    protected final TemplateService templateService;

    public AbstractRequestHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService) {
        this.contextFactory = contextFactory;
        this.sessionService = sessionService;
        this.templateService = templateService;
    }

    public abstract boolean matches(String path);

    public HttpResponse handleRequest(HttpRequest request) {
        BlogContext blogContext = contextFactory.createContext();
        SessionContext sessionContext = new SessionContext();

        String sessionId = getSessionId(request);
        String sessionUsername = sessionService.getUsernameForSession(sessionId);
        boolean isLoggedIn = sessionUsername != null;

        sessionContext.setLoggedIn(isLoggedIn);
        sessionContext.setSessionId(sessionId);
        blogContext.setLoggedIn(isLoggedIn);

        if (isLoggedIn) {
            blogContext.setUsername(sessionUsername);
        }

        HttpResponse response = handleRequest(blogContext, sessionContext, request);

        if (isLoggedIn && !sessionContext.isLoggingOut()) {
            sessionService.refreshSession(sessionId);
        }

        return response;
    }

    protected abstract HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request);

    protected HttpResponse createHttpResponseFromContext(BlogContext context, SessionContext session) {
        HttpResponse response = new HttpResponse();

        response.setStatus(HttpStatus.OK);
        response.setContentType("text/html");
        response.setBody(templateService.render(PAGE_TEMPLATE, context));

        return response;
    }

    protected HttpResponse createClientError(HttpStatus status, String message) {
        HttpResponse response = new HttpResponse();
        response.setStatus(status);
        try {
            // A fresh context defaults to the Error page content
            BlogContext errorContext = contextFactory.createContext();
            errorContext.addErrorMessage(message);
            response.setContentType("text/html");
            response.setBody(templateService.render(PAGE_TEMPLATE, errorContext));
        } catch (RuntimeException e) {
            response.setContentType("text/plain; charset=UTF-8");
            response.setBody(message);
        }
        return response;
    }

    protected String getSessionId(HttpRequest request) {
        List<Cookie> cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("session")) {
                return cookie.getValue();
            }
        }
        return null;
    }

    protected String getSessionUsername(HttpRequest request) {
        String sessionId = getSessionId(request);
        if (sessionId == null) {
            return null;
        }
        return sessionService.getUsernameForSession(sessionId);
    }

    protected Cookie configureSessionCookie(Cookie cookie) {
        return cookie.setPath("/")
                .setHttpOnly(true)
                .setSecure(Boolean.parseBoolean(Config.SESSION_COOKIE_SECURE.value()))
                .setSameSite("Lax");
    }

}
