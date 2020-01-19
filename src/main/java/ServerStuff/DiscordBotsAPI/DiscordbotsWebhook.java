package ServerStuff.DiscordBotsAPI;

import ServerStuff.Server.WebhookServer;

import java.net.Socket;

public class DiscordbotsWebhook extends WebhookServer {

    public DiscordbotsWebhook() {
        super(9998);
    }

    @Override
    public void onServerStart() {
        System.out.println("DiscordBots Server is running!");
    }

    @Override
    public void startSession(Socket socket) {
        new DiscordbotsWebhookSession(socket);
    }
}
