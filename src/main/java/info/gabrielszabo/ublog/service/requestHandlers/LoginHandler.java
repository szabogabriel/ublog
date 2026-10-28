package info.gabrielszabo.ublog.service.requestHandlers;

import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.UserService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.template.TemplateService;

public class LoginHandler extends AbstractRequestHandler {

    public LoginHandler(BlogContextFactory contextFactory, SessionService sessionService, TemplateService templateService, UserService userService) {
        super(contextFactory, sessionService, templateService, userService);
    }

    @Override
    public boolean matches(String path) {
        return path.equals("/login");
    }

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        context.setIsLogin();

        return createHttpResponseFromContext(context, session);
    }

    
}
