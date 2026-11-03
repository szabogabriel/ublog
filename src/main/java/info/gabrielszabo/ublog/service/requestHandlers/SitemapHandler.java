package info.gabrielszabo.ublog.service.requestHandlers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import info.gabrielszabo.ublog.content.ContentProvider;
import info.gabrielszabo.ublog.domain.Category;
import info.gabrielszabo.ublog.domain.EntryDescriptor;
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

public class SitemapHandler extends AbstractRequestHandler {

    private final ContentProvider contentProvider;

    public SitemapHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService, UserService userService, ContentProvider contentProvider) {
        super(contextFactory, sessionService, templateService, userService);
        this.contentProvider = contentProvider;
    }

    @Override
    public boolean matches(String path) {
        return path.equals("/sitemap.xml");
    }

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        if (request.getMethod() != HttpMethods.GET && request.getMethod() != HttpMethods.HEAD) {
            HttpResponse response = createClientError(HttpStatus.METHOD_NOT_ALLOWED, "sitemap.xml requires GET");
            response.addHeader("Allow", "GET, HEAD");
            return response;
        }

        String base = SiteBaseUrl.resolve(request);
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        appendUrl(xml, base + "/");

        // Only public content: unlisted entries are never included.
        for (EntryDescriptor quickLink : contentProvider.getQuickLinks()) {
            appendUrl(xml, base + "/" + encode(quickLink.getName()));
        }
        for (Category category : contentProvider.getCategories()) {
            String categoryPath = base + "/" + encode(category.getName());
            appendUrl(xml, categoryPath);
            for (EntryDescriptor entry : contentProvider.getEntries(category, false)) {
                appendUrl(xml, categoryPath + "/" + encode(entry.getName()));
            }
        }
        xml.append("</urlset>\n");

        HttpResponse response = new HttpResponse();
        response.setStatus(HttpStatus.OK);
        response.setContentType("application/xml; charset=UTF-8");
        response.setBody(xml.toString());
        return response;
    }

    private static void appendUrl(StringBuilder xml, String url) {
        xml.append("  <url><loc>").append(escapeXml(url)).append("</loc></url>\n");
    }

    private static String encode(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
