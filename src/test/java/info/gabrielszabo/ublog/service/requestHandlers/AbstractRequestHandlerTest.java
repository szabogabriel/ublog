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
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
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
                });

        HttpResponse response = handler.clientError(HttpStatus.FORBIDDEN, "Forbidden");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        response.writeResponse(output);

        String responseText = output.toString(StandardCharsets.UTF_8);
        assertTrue(responseText.startsWith("HTTP/1.1 403 Forbidden\r\n"));
    }

    private static final class TestRequestHandler extends AbstractRequestHandler {

        private TestRequestHandler(BlogContextFactory contextFactory, SessionService sessionService,
                TemplateService templateService) {
            super(contextFactory, sessionService, templateService);
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