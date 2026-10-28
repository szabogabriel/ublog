package info.gabrielszabo.ublog.service.requestHandlers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.server.http.HttpMethods;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.server.http.HttpStatus;
import info.gabrielszabo.ublog.server.http.MultipartStreamReader;
import info.gabrielszabo.ublog.service.ImageService;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.UserService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.template.TemplateService;

/** JSON endpoint used by the entry editor: GET lists images, POST uploads or deletes one. */
public class ImageManageHandler extends AbstractRequestHandler {

    private static final String PATH_PREFIX = "/imagemanage/";

    private final ImageService imageService;

    public ImageManageHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService, UserService userService, ImageService imageService) {
        super(contextFactory, sessionService, templateService, userService);
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
        if (!session.isEditor()) {
            return createClientError(HttpStatus.FORBIDDEN, "User not an editor");
        }
        String[] parts = request.getPath().substring(PATH_PREFIX.length()).split("/", -1);
        if (parts.length < 1 || parts.length > 2) {
            return json(HttpStatus.NOT_FOUND, null, null, "Unknown entry");
        }
        // One segment is a root entry; an empty one is the image folder root.
        String topic = parts.length == 2 ? parts[0] : "";
        String entry = parts[parts.length - 1];

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
        if (!MultipartStreamReader.isMultipart(request.getContentType())) {
            return json(HttpStatus.UNSUPPORTED_MEDIA_TYPE, null, null, "Expected multipart/form-data");
        }

        UploadSink sink = new UploadSink(session.getSessionId(), topic, entry);
        try {
            MultipartStreamReader.parse(request.getContentType(), request.getBodyStream(), sink);
            if (!sessionService.isCsrfTokenValid(session.getSessionId(), sink.fields.get("csrfToken"))) {
                return json(HttpStatus.FORBIDDEN, null, null, "Invalid CSRF token");
            }

            String action = sink.fields.get("action");
            String error;
            if ("delete".equals(action)) {
                error = imageService.delete(topic, entry, sink.fields.get("fileName"));
            } else if ("upload".equals(action)) {
                error = sink.uploadFile == null ? "Please choose an image file."
                        : imageService.commitUpload(topic, entry, sink.uploadName, sink.uploadFile);
            } else {
                return json(HttpStatus.BAD_REQUEST, null, null, "Unknown action");
            }
            return json(error == null ? HttpStatus.OK : HttpStatus.BAD_REQUEST, topic, entry, error);
        } catch (Rejected e) {
            return json(e.status, null, null, e.getMessage());
        } catch (IllegalArgumentException e) {
            return json(HttpStatus.BAD_REQUEST, null, null, "Malformed multipart request");
        } catch (IOException e) {
            return json(HttpStatus.BAD_REQUEST, null, null, "Upload failed");
        } finally {
            if (sink.uploadFile != null) {
                imageService.discardUpload(sink.uploadFile);
            }
        }
    }

    private static final class Rejected extends RuntimeException {
        private final HttpStatus status;

        Rejected(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }
    }

    // The CSRF token and action must precede the file part so a bad request is refused before any bytes are stored.
    private final class UploadSink implements MultipartStreamReader.Sink {
        private final String sessionId;
        private final String topic;
        private final String entry;
        private final Map<String, String> fields = new HashMap<>();
        private Path uploadFile;
        private String uploadName;

        UploadSink(String sessionId, String topic, String entry) {
            this.sessionId = sessionId;
            this.topic = topic;
            this.entry = entry;
        }

        @Override
        public void field(String name, String value) {
            if (fields.putIfAbsent(name, value) != null) {
                throw new IllegalArgumentException("Duplicate form field");
            }
        }

        @Override
        public OutputStream file(String fieldName, String fileName) throws IOException {
            if (!"file".equals(fieldName) || uploadFile != null) {
                throw new IllegalArgumentException("Unexpected file part");
            }
            if (!sessionService.isCsrfTokenValid(sessionId, fields.get("csrfToken"))) {
                throw new Rejected(HttpStatus.FORBIDDEN, "Invalid CSRF token");
            }
            if (!"upload".equals(fields.get("action"))) {
                throw new Rejected(HttpStatus.BAD_REQUEST, "Unknown action");
            }
            try {
                uploadFile = imageService.createUploadFile(topic, entry, fileName);
            } catch (IllegalArgumentException e) {
                throw new Rejected(HttpStatus.BAD_REQUEST, e.getMessage());
            }
            uploadName = fileName;
            return Files.newOutputStream(uploadFile);
        }
    }

    private HttpResponse json(HttpStatus status, String topic, String entry, String error) {
        StringBuilder body = new StringBuilder("{");
        List<String> images = topic == null ? null : imageService.list(topic, entry);
        if (images != null) {
            String base = "/image/" + (topic.isEmpty() ? "" : topic + "/")
                    + (entry.isBlank() ? "" : ImageService.normalizeEntry(entry) + "/");
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
