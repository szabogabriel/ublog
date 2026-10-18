package info.gabrielszabo.ublog.service.requestHandlers;

import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.server.http.Cookie;
import info.gabrielszabo.ublog.server.http.HttpMethods;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.template.TemplateService;

public class LogoutHandler extends AbstractRequestHandler {

    public LogoutHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService) {
        super(contextFactory, sessionService, templateService);
    }

    @Override
    public boolean matches(String path) {
        return path.equals("/logout");
    }

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        if (request.getMethod() != HttpMethods.POST) {
            HttpResponse response = createClientError(405, "Logout requires POST");
            response.addHeader("Allow", "POST");
            return response;
        }

        sessionService.invalidateSession(session.getSessionId());
        session.setLoggingOut(true);
        session.setLoggedIn(false);
        context.setLoggingOut();
        context.setLoggedIn(false);
        context.setUsername(getSessionUsername(request));
        context.addIndexToContext();

        HttpResponse response = createHttpResponseFromContext(context, session);

        if (session.isLoggingOut()) {
            response.addCookie(createSessionInvalidationCookie());
        } 

        return response;
    }

    private Cookie createSessionInvalidationCookie() {
        return configureSessionCookie(new Cookie("session", "", 0));
    }
}