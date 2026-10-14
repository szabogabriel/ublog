package info.gabrielszabo.ublog.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import info.gabrielszabo.ublog.config.Config;
import info.gabrielszabo.ublog.server.socket.SocketHandler;
import info.gabrielszabo.ublog.service.log.LogService;

public class Server {
    private LogService logService;
    private SocketHandler socketHandler;

    public Server(LogService logService, SocketHandler socketHandler) {
        this(logService, socketHandler, Integer.parseInt(Config.SERVER_PORT.value()));
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public Server(LogService logService, SocketHandler socketHandler, int port) {
        this.logService = logService;
        this.socketHandler = socketHandler;
        this.logService.logInfo("Starting server ...");
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            this.logService.logInfo("Server started on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                this.logService.logInfo("Accepted a connection");
                this.socketHandler.handle(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
