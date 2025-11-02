package info.gabrielszabo.ublog.service;

import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpRequestHandler;
import info.gabrielszabo.ublog.server.http.HttpResponse;

public class BlogRequestHandler implements HttpRequestHandler {
    private static final String PAGE_TEMPLATE = "index.template";

    private final BlogContextFactory contextFactory;
    private final TemplateService templateService;

    public BlogRequestHandler(BlogContextFactory contextFactory, TemplateService templateService) {
        this.contextFactory = contextFactory;
        this.templateService = templateService;
    }

    @Override
    public HttpResponse handleRequest(HttpRequest request) {
        BlogContext context = contextFactory.createContext();

        String requestedPath = request.getPath();

        if (isIndex(requestedPath)) {
            context.addIndexToContext();
        } else {
            String[] pathParts = requestedPath.split("/");
            if (pathParts.length == 2) {
                String entryName = pathParts[1];
                context.setRequestedSingleEntry(entryName);
            } else  if (pathParts.length == 3) {
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

        return response;
    }

    private boolean isIndex(String requestPath) {
        return requestPath.equals("") || requestPath.equals("/") || requestPath.equals("/index");
    }

 
    
}
