package info.gabrielszabo.ublog.server.socket;

import java.net.Socket;

public interface SocketHandler {

    void handle(Socket socket);
    
}
