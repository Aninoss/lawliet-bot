package events.discordevents.serverjoin;

import core.DiscordApiManager;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ServerJoinAbstract;
import org.javacord.api.event.server.ServerJoinEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEvent
public class ServerJoinNotifyBotOwner extends ServerJoinAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerJoinNotifyBotOwner.class);

    @Override
    public boolean onServerJoin(ServerJoinEvent event) throws Throwable {
        if (event.getServer().getMemberCount() >= 1000)
            DiscordApiManager.getInstance().fetchOwner().get().sendMessage("**+++** " + StringUtil.escapeMarkdown(event.getServer().getName()) + " (" + event.getServer().getMemberCount() + ")").exceptionally(ExceptionLogger.get());

        LOGGER.info("+++ {} ({})", event.getServer().getName(), event.getServer().getMemberCount());
        return true;
    }

}
