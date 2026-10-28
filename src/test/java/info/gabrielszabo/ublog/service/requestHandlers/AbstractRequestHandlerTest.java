package info.gabrielszabo.ublog.service.requestHandlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

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

class AbstractRequestHandlerTest {

    @Test
    void clientErrorResponseWritesRequestedStatusCode() throws IOException {
        BlogContext context = (BlogContext) Proxy.newProxyInstance(
                BlogContext.class.getClassLoader(), new Class<?>[] { BlogContext.class },
                (proxy, method, args) -> null);
        TestRequestHandler handler = new TestRequestHandler(() -> context, new SessionService(),
                new TemplateService() {
                    @Override
                    public String render(String template, BlogContext blogContext) {
                        return "error page";
                    }

                    @Override
                    public Properties getTemplateProperties() {
                        return new Properties();
                    }

                    @Override
                    public void saveTemplateProperties(Properties properties) {
                    }
                }, new UserService(new LogService() {
                    @Override
                    public void logMessage(LogService.LogLevel level, String message) {
                    }

                    @Override
                    public void logInfo(String info) {
                    }

                    @Override
                    public void logError(String error) {
                    }

                    @Override
                    public void logDebug(String debugInfo) {
                    }

                    @Override
                    public void logWarning(String warning) {
                    }

                    @Override
                    public void logTrace(String traceInfo) {
                    }

                }));

        HttpResponse response = handler.clientError(HttpStatus.FORBIDDEN, "Forbidden");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        response.writeResponse(output);

        String responseText = output.toString(StandardCharsets.UTF_8);
        assertTrue(responseText.startsWith("HTTP/1.1 403 Forbidden\r\n"));
        assertTrue(responseText.contains("\r\nConnection: close\r\n"));
    }

    private static final class TestRequestHandler extends AbstractRequestHandler {

        private TestRequestHandler(BlogContextFactory contextFactory, SessionService sessionService,
                TemplateService templateService, UserService userService) {
            super(contextFactory, sessionService, templateService, userService);
        }

        private HttpResponse clientError(HttpStatus status, String message) {
            return createClientError(status, message);
        }

        @Override
        public boolean matches(String path) {
            return false;
        }

        @Override
        protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
            throw new UnsupportedOperationException();
        }
    }
}