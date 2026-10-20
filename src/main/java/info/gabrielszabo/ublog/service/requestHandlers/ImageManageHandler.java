package info.gabrielszabo.ublog.service.requestHandlers;

import java.util.List;

import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.server.http.HttpMethods;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.server.http.HttpStatus;
import info.gabrielszabo.ublog.server.http.MultipartForm;
import info.gabrielszabo.ublog.service.ImageService;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.template.TemplateService;

/** JSON endpoint used by the entry editor: GET lists images, POST uploads or deletes one. */
public class ImageManageHandler extends AbstractRequestHandler {

    private static final String PATH_PREFIX = "/imagemanage/";

    private final ImageService imageService;

    public ImageManageHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService, ImageService imageService) {
        super(contextFactory, sessionService, templateService);
        this.imageService = imageService;
    }

    @Override
    public boolean matches(String path) {
        return path.toLowerCase().startsWith(PATH_PREFIX);
    }

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        if (!session.isLoggedIn()) {
            return json(HttpStatus.FORBIDDEN, null, null, "User not logged in");
        }
        String[] parts = request.getPath().substring(PATH_PREFIX.length()).split("/", -1);
        if (parts.length != 2) {
            return json(HttpStatus.NOT_FOUND, null, null, "Unknown entry");
        }
        String topic = parts[0];
        String entry = parts[1];

        if (request.getMethod() == HttpMethods.GET) {
            List<String> images = imageService.list(topic, entry);
            return images == null ? json(HttpStatus.NOT_FOUND, null, null, "Unknown topic or entry")
                    : json(HttpStatus.OK, topic, entry, null);
        }
        if (request.getMethod() != HttpMethods.POST) {
            HttpResponse response = json(HttpStatus.METHOD_NOT_ALLOWED, null, null, "Requires GET or POST");
            response.addHeader("Allow", "GET, POST");
            return response;
        }
        if (!MultipartForm.isMultipart(request.getContentType())) {
            return json(HttpStatus.UNSUPPORTED_MEDIA_TYPE, null, null, "Expected multipart/form-data");
        }

        MultipartForm form;
        try {
            form = MultipartForm.parse(request.getContentType(), request.getBodyBytes());
        } catch (IllegalArgumentException e) {
            return json(HttpStatus.BAD_REQUEST, null, null, "Malformed multipart request");
        }
        if (!sessionService.isCsrfTokenValid(session.getSessionId(), form.get("csrfToken"))) {
            return json(HttpStatus.FORBIDDEN, null, null, "Invalid CSRF token");
        }

        String error;
        if ("delete".equals(form.get("action"))) {
            error = imageService.delete(topic, entry, form.get("fileName"));
        } else if ("upload".equals(form.get("action"))) {
            error = imageService.save(topic, entry, form.getFile("file"));
        } else {
            return json(HttpStatus.BAD_REQUEST, null, null, "Unknown action");
        }
        return json(error == null ? HttpStatus.OK : HttpStatus.BAD_REQUEST, topic, entry, error);
    }

    private HttpResponse json(HttpStatus status, String topic, String entry, String error) {
        StringBuilder body = new StringBuilder("{");
        List<String> images = topic == null ? null : imageService.list(topic, entry);
        if (images != null) {
            String base = "/image/" + topic + "/" + ImageService.normalizeEntry(entry) + "/";
            body.append("\"images\":[");
            for (int i = 0; i < images.size(); i++) {
                if (i > 0) {
                    body.append(',');
                }
                body.append("{\"name\":\"").append(escape(images.get(i))).append("\",\"url\":\"")
                        .append(escape(base + images.get(i))).append("\"}");
            }
            body.append(']');
        }
        if (error != null) {
            if (images != null) {
                body.append(',');
            }
            body.append("\"error\":\"").append(escape(error)).append('"');
        }
        body.append('}');

        HttpResponse response = new HttpResponse();
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.setBody(body.toString());
        return response;
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c < 0x20) {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
