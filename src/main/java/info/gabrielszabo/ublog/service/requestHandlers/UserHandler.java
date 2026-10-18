package info.gabrielszabo.ublog.service.requestHandlers;

import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.domain.User;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.UserService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.template.TemplateService;
import java.util.Optional;

public class UserHandler extends AbstractRequestHandler {
    
    private final UserService userService;

    public UserHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService, UserService userService) {
        super(contextFactory, sessionService, templateService);
        this.userService = userService;
    }

    @Override
    public boolean matches(String path) {
        return path.equals("/user");
    }

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        if (!session.isLoggedIn()) {
            return createClientError(403, "User not logged in");
        }
        
        Optional<User> userOpt = userService.getUser(context.getUsername());
        
        if (userOpt.isEmpty()) {
            return createClientError(404, "User not found");
        }

        context.setIsUserDetails();
        context.setUserDetails(userOpt.get());

        return createHttpResponseFromContext(context, session);
    }
    
}
