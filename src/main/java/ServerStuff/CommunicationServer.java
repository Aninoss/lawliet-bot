package ServerStuff;

import Core.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class CommunicationServer {

    private int port;
    private final byte OUT_HEARTBEAT = 0x1;
    private final byte OUT_CONNECTED = 0x2;
    final static Logger LOGGER = LoggerFactory.getLogger(CommunicationServer.class);

    public CommunicationServer(int port) {
        this.port = port;
        Thread t = new Thread(this::run);
        t.setName("communication_server");
        t.start();
    }

    private void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port, 0, InetAddress.getLoopbackAddress());
            LOGGER.debug("Communication has been started");

            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    OutputStream os = socket.getOutputStream();
                    os.write(OUT_HEARTBEAT | OUT_CONNECTED);
                    os.flush();
                } catch (IOException e) {
                    LOGGER.error("Could not send web communication data", e);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Exception in communication server", e);
        }
    }
}
