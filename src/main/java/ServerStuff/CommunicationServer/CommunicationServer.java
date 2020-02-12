package ServerStuff.CommunicationServer;

import CommandSupporters.CommandContainer;
import Constants.Settings;
import General.Bot;
import General.Connector;
import General.DiscordApiCollection;
import General.ExceptionHandler;
import General.RunningCommands.RunningCommandManager;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.UserStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

public class CommunicationServer {

    private int port;
    private final byte OUT_HEARTBEAT = 0x1;
    private final byte OUT_CONNECTED = 0x2;

    public CommunicationServer(int port) {
        this.port = port;
        Thread t = new Thread(this::run);
        t.setName("communication_server");
        t.start();
    }

    private void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port, 0, InetAddress.getLoopbackAddress());
            System.out.println("Communication Server is running!");

            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    OutputStream os = socket.getOutputStream();
                    os.write(OUT_HEARTBEAT | OUT_CONNECTED);
                    os.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            ExceptionHandler.showErrorLog("Exception in communication server");
            System.exit(-1);
        }
    }
}
