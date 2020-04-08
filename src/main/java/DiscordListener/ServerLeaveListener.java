package DiscordListener;

import Constants.Settings;
import General.DiscordApiCollection;
import General.TextManager;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.ServerLeaveEvent;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ServerLeaveListener {

    public void onServerLeave(ServerLeaveEvent event) throws Exception {
        Server server = event.getServer();
        ServerBean serverBean = DBServer.getInstance().getBean(server.getId());
        String text = TextManager.getString(serverBean.getLocale(), TextManager.GENERAL, "kick_message", String.format(Settings.FEEDBACK_WEBSITE, event.getServer().getId()));

        serverBean.getWebhookUrl().ifPresent(webhookUrl -> {
            try {
                DiscordApiCollection.getInstance().sendToWebhook(server, webhookUrl, text).get();
                DiscordApiCollection.getInstance().removeWebhook(webhookUrl);
            } catch (IOException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        if (event.getServer().getMembers().size() >= 100)
            DiscordApiCollection.getInstance().getOwner().sendMessage("**---** " + event.getServer().getName() + " (" + event.getServer().getMembers().size() + ")");
    }
}
