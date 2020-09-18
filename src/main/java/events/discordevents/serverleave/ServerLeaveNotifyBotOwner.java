package events.discordevents.serverleave;

import core.DiscordApiCollection;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ServerLeaveAbstract;
import org.javacord.api.event.server.ServerLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEvent
public class ServerLeaveNotifyBotOwner extends ServerLeaveAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerLeaveNotifyBotOwner.class);

    @Override
    public boolean onServerLeave(ServerLeaveEvent event) throws Throwable {
        if (event.getServer().getMemberCount() >= 1000)
            DiscordApiCollection.getInstance().getOwner().sendMessage("**---** " + StringUtil.escapeMarkdown(event.getServer().getName()) + " (" + event.getServer().getMemberCount() + ")");

        LOGGER.info("--- {} ({})", event.getServer().getName(), event.getServer().getMemberCount());
        return true;
    }

}
