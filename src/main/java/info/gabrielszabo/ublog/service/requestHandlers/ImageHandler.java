package info.gabrielszabo.ublog.service.requestHandlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

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

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        HttpResponse response = new HttpResponse();
        String imageName = getImageNameFromPath(request.getPath());
        if (imageName == null) {
            return response;
        }

        String mimeType = MimeUtil.getMimeType(imageName);
        File imageFile = new File(Config.IMAGE_FOLDER.value() + "/" + imageName);
        log.logTrace("Loading image " + imageFile);

        if (!imageFile.exists()) {
            log.logError("Image not found: " + imageName);
            response.setStatusCode(404);
            return response;
        }

        response.setStatusCode(200);
        response.setContentType(mimeType);
        try {
            response.setBody(new FileInputStream(imageFile), (int) imageFile.length());
        } catch (FileNotFoundException e) {
            log.logError("Error loading image: " + imageName);
            e.printStackTrace();
        }
        return response;
    }

    private String getImageNameFromPath(String requestPath) {
        if (requestPath.startsWith("/image/") && !requestPath.contains("..")) {
            return requestPath.substring("/image/".length());
        }
        return null;
    }
}