package info.gabrielszabo.ublog.service.requestHandlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import info.gabrielszabo.ublog.config.Config;
import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.log.LogService;
import info.gabrielszabo.ublog.service.template.TemplateService;
import info.gabrielszabo.ublog.utils.MimeUtil;

public class ImageHandler extends AbstractRequestHandler {

    private final LogService log;

    public ImageHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService, LogService log) {
        super(contextFactory, sessionService, templateService);
        this.log = log;
    }

    @Override
    public boolean matches(String path) {
        return path.startsWith("/image/");
    }

    /**
     * (non-Javadoc)
     * No need for BlogContext or SessionContext parameters in this handler. Hence
     * overriding the whole handleRequest method.
     */
    @Override
    public HttpResponse handleRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        String imageName = getImageNameFromPath(request.getPath());
        Path imagePath = resolveImage(imageName);

        if (imagePath == null || imagePath.toFile().length() > Integer.MAX_VALUE) {
            response.setStatusCode(404);
            response.setContentType("text/plain; charset=UTF-8");
            response.setBody("Image not found or too large");
            return response;
        }

        log.logTrace("Loading image " + imagePath);

        try {
            response.setStatusCode(200);
            response.setContentType(MimeUtil.getMimeType(imageName));    
            response.setBody(Files.newInputStream(imagePath, LinkOption.NOFOLLOW_LINKS), (int) imagePath.toFile()  .length());
        } catch (IOException e) {
            log.logError("Error loading image: " + imageName);

            response.setStatusCode(404);
            response.setContentType("text/plain; charset=UTF-8");
            response.setBody("Error when loading image.");
            return response;
        }

        String sessionId = getSessionId(request);
        String sessionUsername = sessionService.getUsernameForSession(sessionId);
        boolean isLoggedIn = sessionUsername != null;
        if (isLoggedIn) {
            sessionService.refreshSession(sessionId);
        }

        return response;
    }

    private Path resolveImage(String imageName) {
        if (imageName == null || imageName.isEmpty()) {
            return null;
        }
        try {
            Path root = Path.of(Config.IMAGE_FOLDER.value()).toRealPath();

            Path candidate = root.resolve(imageName).normalize();

            // Prevent ../ traversal
            if (!candidate.startsWith(root)) {
                throw new IOException("Invalid image path");
            }

            // Reject symbolic links in every path component
            Path current = root;

            for (Path part : root.relativize(candidate)) {
                current = current.resolve(part);

                if (Files.isSymbolicLink(current)) {
                    throw new IOException("Symbolic links are not allowed");
                }
            }

            if (!Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Image not found");
            }

            return candidate;
        } catch (IOException e) {
            log.logError("Error resolving image path: " + imageName);
            e.printStackTrace();
            return null;
        }
    }

    private String getImageNameFromPath(String requestPath) {
        if (requestPath.startsWith("/image/") && !requestPath.contains("..")) {
            return requestPath.substring("/image/".length());
        }
        return null;
    }

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'handleRequest'");
    }
}