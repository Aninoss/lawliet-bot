package DiscordListener;

import Constants.Settings;
import Core.DiscordApiCollection;
import Core.TextManager;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.ServerLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ServerLeaveListener {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerLeaveListener.class);

    public void onServerLeave(ServerLeaveEvent event) {
        try {
            Server server = event.getServer();
            ServerBean serverBean = DBServer.getInstance().getBean(server.getId());
            String text = TextManager.getString(serverBean.getLocale(), TextManager.GENERAL, "kick_message", String.format(Settings.FEEDBACK_WEBSITE, event.getServer().getId()));

            serverBean.getWebhookUrl().ifPresent(webhookUrl -> {
                try {
                    DiscordApiCollection.getInstance().sendToWebhook(server, webhookUrl, text).get();
                    DiscordApiCollection.getInstance().removeWebhook(webhookUrl);
                } catch (IOException | InterruptedException | ExecutionException e) {
                    LOGGER.error("Could not post on webhook", e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Error on server leave", e);
        }

        if (event.getServer().getMemberCount() >= 500)
            DiscordApiCollection.getInstance().getOwner().sendMessage("**---** " + event.getServer().getName() + " (" + event.getServer().getMemberCount() + ")");

        LOGGER.info("--- {} ({})", event.getServer().getName(), event.getServer().getMemberCount());

        DBServer.getInstance().remove(event.getServer().getId());
    }
}
