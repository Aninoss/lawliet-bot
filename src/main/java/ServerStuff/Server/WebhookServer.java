package ServerStuff.Server;

import ServerStuff.DiscordBotsAPI.DiscordbotsWebhookSession;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

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
            onServerStart();
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    new DiscordbotsWebhookSession(socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
