package info.gabrielszabo.ublog.service.requestHandlers;

import java.util.Optional;

import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.domain.User;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.server.http.HttpStatus;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.UserService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.template.TemplateService;

public class UserHandler extends AbstractRequestHandler {
    
    public UserHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService, UserService userService) {
        super(contextFactory, sessionService, templateService, userService);
    }

    @Override
    public boolean matches(String path) {
        return path.equals("/user");
    }

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        if (!session.isLoggedIn()) {
            return createClientError(HttpStatus.FORBIDDEN, "User not logged in");
        }
        
        Optional<User> userOpt = userService.getUser(context.getUsername());
        
        if (userOpt.isEmpty()) {
            return createClientError(HttpStatus.NOT_FOUND, "User not found");
        }

        context.setIsUserDetails();
        context.setCsrfToken(sessionService.getCsrfToken(session.getSessionId()));
        context.setUserDetails(userOpt.get());
        context.setTemplateProperties(getTemplateProperties());

        return createHttpResponseFromContext(context, session);
    }
    
}
