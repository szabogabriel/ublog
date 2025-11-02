package info.gabrielszabo.ublog.server.socket;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import info.gabrielszabo.ublog.config.Config;
import info.gabrielszabo.ublog.content.MarkdownContentProvider;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpRequestHandler;
import info.gabrielszabo.ublog.server.http.HttpResponse;

public class PooledSocketHandler implements SocketHandler {

    private ThreadPoolExecutor executor;
    private HttpRequestHandler requestHandler;

    public PooledSocketHandler(MarkdownContentProvider fileSystemHandler, HttpRequestHandler requestHandler) {
        // Constructor implementation
        this.requestHandler = requestHandler;
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Integer.parseInt(Config.SOCKET_HANDLER_THREAD_POOL_SIZE.value()));
    }

    @Override
    public void handle(Socket socket) {
        executor.execute(() -> {
            try {
                System.out.println("Handling socket: " + socket);
                InputStream inStream = socket.getInputStream();
                OutputStream outStream = socket.getOutputStream();

                // Read the incoming request
                byte[] buffer = new byte[4096];
                int bytesRead;

                StringBuilder requestBuilder = new StringBuilder();
                do {
                    bytesRead = inStream.read(buffer);
                    String request = bytesRead > 0 ? new String(buffer, 0, bytesRead) : "";
                    requestBuilder.append(request);
                } while (bytesRead == 4096);

                String fullRequest = requestBuilder.toString();

                HttpRequest httpRequest = new HttpRequest(fullRequest);
                HttpResponse httpResponse = requestHandler.handleRequest(httpRequest);

                // Write the response
                String response = httpResponse.toHttpResponseMessage();
                outStream.write(response.getBytes());
                outStream.flush();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
