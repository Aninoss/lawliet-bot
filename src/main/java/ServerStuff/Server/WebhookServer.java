package ServerStuff.Server;

import General.ExceptionHandler;
import ServerStuff.DiscordBotsAPI.DiscordbotsWebhookSession;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;

public class WebhookServer {
    private int port;

    public WebhookServer(int port) {
        this.port = port;
        Thread t = new Thread(this::run);
        t.setName("webhook_server");
        t.start();
    }

    public void onServerStart() {
        System.out.println("Server is running!");
    }

    public void startSession(Socket socket) {
        new WebhookServerSession(socket, "");
    }

    private void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    serverSocket.close();
                } catch (IOException e) { /* failed */ }
            }));
            onServerStart();
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    startSession(socket);
                } catch (IOException e) {
                    e.printStackTrace();
                    ExceptionHandler.showErrorLog("Webhook Server Error!");
                    System.exit(-1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            ExceptionHandler.showErrorLog("Exception in webhook server");
            System.exit(-1);
        }
    }
}
