package info.gabrielszabo.ublog.service;

import java.util.ArrayList;
import java.util.List;

import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpRequestHandler;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.log.LogService;
import info.gabrielszabo.ublog.service.requestHandlers.AbstractRequestHandler;
import info.gabrielszabo.ublog.service.requestHandlers.ContentHandler;
import info.gabrielszabo.ublog.service.requestHandlers.CreateEntryHandler;
import info.gabrielszabo.ublog.service.requestHandlers.CreateTopicHandler;
import info.gabrielszabo.ublog.service.requestHandlers.EditEntryHandler;
import info.gabrielszabo.ublog.service.requestHandlers.ImageHandler;
import info.gabrielszabo.ublog.service.requestHandlers.IndexHandler;
import info.gabrielszabo.ublog.service.requestHandlers.LoginActionHandler;
import info.gabrielszabo.ublog.service.requestHandlers.LoginHandler;
import info.gabrielszabo.ublog.service.requestHandlers.LogoutHandler;
import info.gabrielszabo.ublog.service.requestHandlers.PasswordChangeHandler;
import info.gabrielszabo.ublog.service.requestHandlers.UserHandler;
import info.gabrielszabo.ublog.service.template.TemplateService;

public class RequestHandlerService implements HttpRequestHandler {
    private final LogService log;

    private final List<AbstractRequestHandler> requestHandlers = new ArrayList<>();

    public RequestHandlerService(BlogContextFactory contextFactory, TemplateService templateService,
            UserService userService, LoginService loginService, SessionService sessionService, LogService log, ContentService contentService) {
        this.log = log;

        requestHandlers.add(new IndexHandler(contextFactory, sessionService, templateService));
        requestHandlers.add(new ImageHandler(contextFactory, sessionService, templateService, log));
        requestHandlers.add(new UserHandler(contextFactory, sessionService, templateService, userService));
        requestHandlers.add(new LoginHandler(contextFactory, sessionService, templateService));
        requestHandlers.add(new LoginActionHandler(contextFactory, sessionService, templateService,
                userService, loginService, log));
        requestHandlers.add(new PasswordChangeHandler(contextFactory, sessionService, templateService,
            userService, loginService));
        requestHandlers.add(new LogoutHandler(contextFactory, sessionService, templateService));
        requestHandlers.add(new CreateTopicHandler(contextFactory, sessionService, templateService, contentService));
        requestHandlers.add(new CreateEntryHandler(contextFactory, sessionService, templateService, contentService));
        requestHandlers.add(new EditEntryHandler(contextFactory, sessionService, templateService, contentService));
        requestHandlers.add(new ContentHandler(contextFactory, sessionService, templateService));
    }

    @Override
    public HttpResponse handleRequest(HttpRequest request) {
        log.logInfo("Handling request " + request.getPath());
        for (AbstractRequestHandler handler : requestHandlers) {
            if (handler.matches(request.getPath())) {
                return handler.handleRequest(request);
            }
        }

        HttpResponse response = new HttpResponse();
        response.setStatusCode(404);
        response.setContentType("text/plain; charset=UTF-8");
        response.setBody("No request handler matched the request path");
        return response;
    }

}
