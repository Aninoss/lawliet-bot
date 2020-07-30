package DiscordEvents.ServerLeave;

import Core.DiscordApiCollection;
import Core.Utils.StringUtil;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerLeaveAbstract;
import org.javacord.api.event.server.ServerLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEventAnnotation
public class ServerLeaveNotifyBotOwner extends ServerLeaveAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerLeaveNotifyBotOwner.class);

    @Override
    public boolean onServerLeave(ServerLeaveEvent event) throws Throwable {
        if (event.getServer().getMemberCount() >= 500)
            DiscordApiCollection.getInstance().getOwner().sendMessage("**---** " + StringUtil.escapeMarkdown(event.getServer().getName()) + " (" + event.getServer().getMemberCount() + ")");

        LOGGER.info("--- {} ({})", event.getServer().getName(), event.getServer().getMemberCount());
        return true;
    }

}
