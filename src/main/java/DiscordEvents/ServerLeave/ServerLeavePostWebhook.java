package DiscordEvents.ServerLeave;

import Constants.Settings;
import Core.DiscordApiCollection;
import Core.TextManager;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerLeaveAbstract;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.ServerLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

@DiscordEventAnnotation
public class ServerLeavePostWebhook extends ServerLeaveAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerLeavePostWebhook.class);

    @Override
    public boolean onServerLeave(ServerLeaveEvent event) throws Throwable {
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

        return true;
    }

}
