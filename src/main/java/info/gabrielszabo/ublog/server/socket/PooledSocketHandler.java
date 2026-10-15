package info.gabrielszabo.ublog.server.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import info.gabrielszabo.ublog.config.Config;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpRequestHandler;
import info.gabrielszabo.ublog.server.http.HttpResponse;

public class PooledSocketHandler implements SocketHandler {

    private static final int SOCKET_READ_TIMEOUT_MILLIS = 10_000;

    private final ThreadPoolExecutor executor;
    private final HttpRequestHandler requestHandler;

    public PooledSocketHandler(HttpRequestHandler requestHandler) {
        // Constructor implementation
        this.requestHandler = requestHandler;
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Integer.parseInt(Config.SOCKET_HANDLER_THREAD_POOL_SIZE.value()));
    }

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void handle(Socket socket) {
        executor.execute(() -> {
            try (Socket client = socket) {
                client.setSoTimeout(SOCKET_READ_TIMEOUT_MILLIS);
                InputStream inStream = client.getInputStream();
                OutputStream outStream = client.getOutputStream();

                HttpRequest httpRequest;
                try {
                    httpRequest = HttpRequest.readFrom(inStream);
                } catch (HttpRequest.RequestException e) {
                    writeErrorResponse(outStream, e.getStatusCode(), e.getMessage());
                    return;
                } catch (SocketTimeoutException e) {
                    writeErrorResponse(outStream, 408, "Request timed out");
                    return;
                }

                try {
                    HttpResponse httpResponse = requestHandler.handleRequest(httpRequest);
                    outStream.write(httpResponse.toHttpResponseMessage().getBytes(StandardCharsets.UTF_8));
                    outStream.flush();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    writeErrorResponse(outStream, 500, "Internal server error");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void writeErrorResponse(OutputStream outStream, int statusCode, String message) throws IOException {
        HttpResponse response = new HttpResponse();
        response.setStatusCode(statusCode);
        response.setContentType("text/plain; charset=UTF-8");
        response.setBody(message);
        outStream.write(response.toHttpResponseMessage().getBytes(StandardCharsets.UTF_8));
        outStream.flush();
    }

}
