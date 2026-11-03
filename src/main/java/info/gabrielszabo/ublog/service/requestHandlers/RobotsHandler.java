package info.gabrielszabo.ublog.service.requestHandlers;

import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.server.http.HttpMethods;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.server.http.HttpStatus;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.UserService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.template.TemplateService;

public class RobotsHandler extends AbstractRequestHandler {

    private static final String[] DISALLOWED = {
        "/unlisted/", "/login", "/loginaction", "/logout", "/user", "/passwordchange",
        "/tokengenerate", "/tokendelete", "/templateproperties", "/mcp", "/createtopic",
        "/createentry/", "/editentry/", "/deleteentry/", "/imagemanage/", "/filemanage/"
    };

    public RobotsHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService, UserService userService) {
        super(contextFactory, sessionService, templateService, userService);
    }

    @Override
    public boolean matches(String path) {
        return path.equals("/robots.txt");
    }

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        if (request.getMethod() != HttpMethods.GET && request.getMethod() != HttpMethods.HEAD) {
            HttpResponse response = createClientError(HttpStatus.METHOD_NOT_ALLOWED, "robots.txt requires GET");
            response.addHeader("Allow", "GET, HEAD");
            return response;
        }

        StringBuilder body = new StringBuilder("User-agent: *\nAllow: /\n");
        for (String path : DISALLOWED) {
            body.append("Disallow: ").append(path).append('\n');
        }
        body.append("\nSitemap: ").append(SiteBaseUrl.resolve(request)).append("/sitemap.xml\n");

        HttpResponse response = new HttpResponse();
        response.setStatus(HttpStatus.OK);
        response.setContentType("text/plain; charset=UTF-8");
        response.setBody(body.toString());
        return response;
    }
}
