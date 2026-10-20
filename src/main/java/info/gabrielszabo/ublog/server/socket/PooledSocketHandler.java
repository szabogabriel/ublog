package info.gabrielszabo.ublog.server.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import info.gabrielszabo.ublog.config.Config;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpRequestHandler;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.server.http.HttpStatus;

public class PooledSocketHandler implements SocketHandler {

    private static final int SOCKET_READ_TIMEOUT_MILLIS = 10_000;

    private final ThreadPoolExecutor executor;
    private final HttpRequestHandler requestHandler;

    public PooledSocketHandler(HttpRequestHandler requestHandler) {
        this.requestHandler = requestHandler;
        int threadCount = Integer.parseInt(Config.SOCKET_HANDLER_THREAD_POOL_SIZE.value());
        int queueCapacity = Integer.parseInt(Config.SOCKET_HANDLER_QUEUE_CAPACITY.value());
        executor = new ThreadPoolExecutor(threadCount, threadCount, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(queueCapacity));
    }

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void handle(Socket socket) {
        try {
            executor.execute(() -> {
                try (Socket client = socket) {
                    client.setSoTimeout(SOCKET_READ_TIMEOUT_MILLIS);
                    InputStream inStream = client.getInputStream();
                    OutputStream outStream = client.getOutputStream();

                    HttpRequest httpRequest;
                    try {
                        httpRequest = HttpRequest.readFrom(inStream);
                    } catch (HttpRequest.RequestException e) {
                        try {
                            writeErrorResponse(outStream, e.getStatus(), e.getMessage());
                        } catch (IOException ignored) {
                            // The client may have closed the connection before sending a full request.
                        }
                        return;
                    } catch (SocketTimeoutException e) {
                        try {
                            writeErrorResponse(outStream, HttpStatus.REQUEST_TIMEOUT, "Request timed out");
                        } catch (IOException ignored) {
                            // Browsers drop idle pre-opened connections, so the peer is often already gone.
                        }
                        return;
                    }

                    try {
                        HttpResponse httpResponse = requestHandler.handleRequest(httpRequest);
                        httpResponse.writeResponse(outStream);
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        writeErrorResponse(outStream, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (RejectedExecutionException e) {
            try (Socket rejected = socket) {
                writeErrorResponse(rejected.getOutputStream(), HttpStatus.SERVICE_UNAVAILABLE, "Server is busy");
            } catch (IOException ignored) {
                // The connection may close before the overload response is written.
            }
        }
    }

    private void writeErrorResponse(OutputStream outStream, HttpStatus status, String message) throws IOException {
        HttpResponse response = new HttpResponse();
        response.setStatus(status);
        response.setContentType("text/plain; charset=UTF-8");
        response.setBody(message);
        response.writeResponse(outStream);
    }

}
