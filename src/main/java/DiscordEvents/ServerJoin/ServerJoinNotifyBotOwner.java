package DiscordEvents.ServerJoin;

import Core.DiscordApiCollection;
import Core.Utils.StringUtil;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerJoinAbstract;
import org.javacord.api.event.server.ServerJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEventAnnotation
public class ServerJoinNotifyBotOwner extends ServerJoinAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerJoinNotifyBotOwner.class);

    @Override
    public boolean onServerJoin(ServerJoinEvent event) throws Throwable {
        if (event.getServer().getMemberCount() >= 500)
            DiscordApiCollection.getInstance().getOwner().sendMessage("**+++** " + StringUtil.escapeMarkdown(event.getServer().getName()) + " (" + event.getServer().getMemberCount() + ")");

        LOGGER.info("+++ {} ({})", event.getServer().getName(), event.getServer().getMemberCount());
        return true;
    }

}
