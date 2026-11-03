package info.gabrielszabo.ublog.service.requestHandlers;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

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
import info.gabrielszabo.ublog.utils.CryptoUtils;

public class TokenHandler extends AbstractRequestHandler {

    private static final String GENERATE_PATH = "/tokengenerate";
    private static final String DELETE_PATH = "/tokendelete";


    public TokenHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService, UserService userService) {
        super(contextFactory, sessionService, templateService, userService);
    }

    @Override
    public boolean matches(String path) {
        return path.equals(GENERATE_PATH) || path.equals(DELETE_PATH);
    }

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        if (!session.isLoggedIn()) {
            return createClientError(HttpStatus.FORBIDDEN, "User not logged in");
        }
        if (request.getMethod() != HttpMethods.POST) {
            HttpResponse response = createClientError(HttpStatus.METHOD_NOT_ALLOWED, "Token management requires POST");
            response.addHeader("Allow", "POST");
            return response;
        }
        if (!sessionService.isCsrfTokenValid(session.getSessionId(), parseCsrfToken(request.getBody()))) {
            return createClientError(HttpStatus.FORBIDDEN, "Invalid CSRF token");
        }

        boolean generate = request.getPath().equals(GENERATE_PATH);
        String plainToken = generate ? CryptoUtils.INSTANCE.generateToken() : null;
        String hash = generate ? CryptoUtils.INSTANCE.hash(plainToken) : null;

        if (!userService.updateToken(context.getUsername(), hash)) {
            return createUserDetailsResponse(context, session, null, "The token could not be updated right now. Please try again.");
        }
        if (generate) {
            return createUserDetailsResponse(context, session, plainToken, null);
        }
        context.addSuccessMessage("Your token has been deleted.");
        return createUserDetailsResponse(context, session, null, null);
    }

    private HttpResponse createUserDetailsResponse(BlogContext context, SessionContext session, String plainToken,
            String error) {
        Optional<User> userOpt = userService.getUser(context.getUsername());
        if (userOpt.isEmpty()) {
            return createClientError(HttpStatus.NOT_FOUND, "User not found");
        }
        context.setIsUserDetails();
        context.setCsrfToken(sessionService.getCsrfToken(session.getSessionId()));
        context.setUserDetails(userOpt.get());
        context.setTemplateProperties(getTemplateProperties());
        if (plainToken != null) {
            context.setGeneratedToken(plainToken);
            context.addSuccessMessage("Your new token has been generated. Copy it now, it will not be shown again.");
        }
        if (error != null) {
            context.addErrorMessage(error);
        }
        return createHttpResponseFromContext(context, session);
    }

    private String parseCsrfToken(String body) {
        if (body == null) {
            return null;
        }
        for (String pair : body.split("&")) {
            int separator = pair.indexOf('=');
            if (separator > 0 && URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8)
                    .equals("csrfToken")) {
                return URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
