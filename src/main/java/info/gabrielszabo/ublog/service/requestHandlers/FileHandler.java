package info.gabrielszabo.ublog.service.requestHandlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import info.gabrielszabo.ublog.config.Config;
import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.server.http.HttpStatus;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.UserService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.log.LogService;
import info.gabrielszabo.ublog.service.template.TemplateService;

/** Serves uploaded files from [FILE_FOLDER] as downloads. */
public class FileHandler extends AbstractRequestHandler {

    private static final String PATH_PREFIX = "/file/";

    private final LogService log;

    public FileHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService, UserService userService, LogService log) {
        super(contextFactory, sessionService, templateService, userService);
        this.log = log;
    }

    @Override
    public boolean matches(String path) {
        return path.startsWith(PATH_PREFIX);
    }

    /** No BlogContext or SessionContext is needed, so the whole handleRequest method is overridden. */
    @Override
    public HttpResponse handleRequest(HttpRequest request) {
        String relative = request.getPath().substring(PATH_PREFIX.length());
        Path filePath = relative.contains("..") ? null : resolveFile(relative);

        if (filePath == null || filePath.toFile().length() > Integer.MAX_VALUE) {
            return createClientError(HttpStatus.NOT_FOUND, "File not found or too large");
        }

        log.logTrace("Loading file " + filePath);

        HttpResponse response = new HttpResponse();
        try {
            response.setStatus(HttpStatus.OK);
            // Uploaded content is untrusted: never let the browser render it from this origin.
            response.setContentType("application/octet-stream");
            response.addHeader("Content-Disposition",
                    "attachment; filename=\"" + filePath.getFileName() + "\"");
            response.addHeader("X-Content-Type-Options", "nosniff");
            response.setBody(Files.newInputStream(filePath, LinkOption.NOFOLLOW_LINKS),
                    (int) filePath.toFile().length());
        } catch (IOException e) {
            log.logError("Error loading file: " + relative);
            return createClientError(HttpStatus.NOT_FOUND, "Error when loading file.");
        }

        String sessionId = getSessionId(request);
        if (sessionService.getUsernameForSession(sessionId) != null) {
            sessionService.refreshSession(sessionId);
        }
        return response;
    }

    private Path resolveFile(String relative) {
        if (relative.isEmpty()) {
            return null;
        }
        try {
            Path root = Path.of(Config.FILE_FOLDER.value()).toRealPath();
            Path candidate = root.resolve(relative).normalize();
            if (!candidate.startsWith(root)) {
                return null;
            }
            Path current = root;
            for (Path part : root.relativize(candidate)) {
                current = current.resolve(part);
                if (Files.isSymbolicLink(current)) {
                    return null;
                }
            }
            return Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS) ? candidate : null;
        } catch (IOException e) {
            log.logError("Error resolving file path: " + relative);
            return null;
        }
    }

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'handleRequest'");
    }
}
