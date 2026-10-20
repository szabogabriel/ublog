package info.gabrielszabo.ublog.service.requestHandlers;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.domain.User;
import info.gabrielszabo.ublog.server.http.HttpMethods;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.server.http.HttpStatus;
import info.gabrielszabo.ublog.service.LoginService;
import info.gabrielszabo.ublog.service.LoginService.PasswordChangeResult;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.UserService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.template.TemplateService;

public class PasswordChangeHandler extends AbstractRequestHandler {

    private final UserService userService;
    private final LoginService loginService;

    public PasswordChangeHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService, UserService userService, LoginService loginService) {
        super(contextFactory, sessionService, templateService);
        this.userService = userService;
        this.loginService = loginService;
    }

    @Override
    public boolean matches(String path) {
        return path.equals("/passwordchange");
    }

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        if (!session.isLoggedIn()) {
            return createClientError(HttpStatus.FORBIDDEN, "User not logged in");
        }
        if (request.getMethod() != HttpMethods.POST) {
            HttpResponse response = createClientError(HttpStatus.METHOD_NOT_ALLOWED, "Password change requires POST");
            response.addHeader("Allow", "POST");
            return response;
        }
        if (!isFormContentType(request.getContentType())) {
            return createClientError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Expected application/x-www-form-urlencoded");
        }

        PasswordChangeForm passwordChangeForm;
        try {
            passwordChangeForm = parsePasswordChangeForm(request.getBody());
        } catch (IllegalArgumentException e) {
            return createUserDetailsResponse(context, session,
                    PasswordChangeMessage.error("Please complete all password fields and make sure the new passwords match."));
        }

        if (!sessionService.isCsrfTokenValid(session.getSessionId(), passwordChangeForm.csrfToken())) {
            return createClientError(HttpStatus.FORBIDDEN, "Invalid CSRF token");
        }

        PasswordChangeResult result = loginService.changePassword(context.getUsername(),
                passwordChangeForm.currentPassword(), passwordChangeForm.newPassword());
        if (result == PasswordChangeResult.INVALID_REQUEST) {
            return createUserDetailsResponse(context, session,
                    PasswordChangeMessage.error("Please complete all password fields and make sure the new passwords match."));
        }
        if (result == PasswordChangeResult.INVALID_CURRENT_PASSWORD) {
            return createUserDetailsResponse(context, session,
                    PasswordChangeMessage.error("The current password you entered is not correct."));
        }
        if (result == PasswordChangeResult.USER_NOT_FOUND) {
            return createClientError(HttpStatus.NOT_FOUND, "User not found");
        }
        if (result == PasswordChangeResult.UPDATE_FAILED) {
            return createUserDetailsResponse(context, session,
                    PasswordChangeMessage.error("The password could not be updated right now. Please try again."));
        }

        return createUserDetailsResponse(context, session,
                PasswordChangeMessage.success("Your password has been changed successfully."));
    }

    private HttpResponse createUserDetailsResponse(BlogContext context, SessionContext session,
            PasswordChangeMessage message) {
        Optional<User> userOpt = userService.getUser(context.getUsername());
        if (userOpt.isEmpty()) {
            return createClientError(HttpStatus.NOT_FOUND, "User not found");
        }

        context.setIsUserDetails();
    context.setCsrfToken(sessionService.getCsrfToken(session.getSessionId()));
        context.setUserDetails(userOpt.get());

        if (message.type() == MessageType.SUCCESS) {
            context.addSuccessMessage(message.value());
        } else if (message.type() == MessageType.INFO) {
            context.addInfoMessage(message.value());
        } else if (message.type() == MessageType.ERROR) {
            context.addErrorMessage(message.value());
        }

        return createHttpResponseFromContext(context, session);
    }

    private boolean isFormContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        int parameterStart = contentType.indexOf(';');
        String mediaType = parameterStart >= 0 ? contentType.substring(0, parameterStart) : contentType;
        return mediaType.trim().equalsIgnoreCase("application/x-www-form-urlencoded");
    }

    private PasswordChangeForm parsePasswordChangeForm(String body) {
        if (body == null || body.isEmpty()) {
            throw new IllegalArgumentException("Missing password change fields");
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

        String currentPassword = fields.get("currentPassword");
        String newPassword = fields.get("newPassword");
        String newPasswordRepeat = fields.get("newPasswordRepeat");
        String csrfToken = fields.get("csrfToken");
        if (currentPassword == null || currentPassword.isEmpty() || newPassword == null || newPassword.isEmpty()
                || newPasswordRepeat == null || newPasswordRepeat.isEmpty()) {
            throw new IllegalArgumentException("Missing password change field");
        }
        if (!newPassword.equals(newPasswordRepeat)) {
            throw new IllegalArgumentException("Mismatched password confirmation");
        }

        return new PasswordChangeForm(currentPassword, newPassword, csrfToken);
    }

    private record PasswordChangeForm(String currentPassword, String newPassword, String csrfToken) {
    }

    private record PasswordChangeMessage(MessageType type, String value) {
        private static PasswordChangeMessage success(String value) {
            return new PasswordChangeMessage(MessageType.SUCCESS, value);
        }

        private static PasswordChangeMessage error(String value) {
            return new PasswordChangeMessage(MessageType.ERROR, value);
        }
    }

    private enum MessageType {
        SUCCESS,
        INFO,
        ERROR
    }
}