package events.discordevents.serverleave;

import core.DiscordApiManager;
import core.MainLogger;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ServerLeaveAbstract;
import org.javacord.api.event.server.ServerLeaveEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEvent
public class ServerLeaveNotifyBotOwner extends ServerLeaveAbstract {

    @Override
    public boolean onServerLeave(ServerLeaveEvent event) throws Throwable {
        if (event.getServer().getMemberCount() >= 5000)
            DiscordApiManager.getInstance().fetchOwner().get().sendMessage("**---** " + StringUtil.escapeMarkdown(event.getServer().getName()) + " (" + event.getServer().getMemberCount() + ")").exceptionally(ExceptionLogger.get());

        MainLogger.get().info("--- {} ({})", event.getServer().getName(), event.getServer().getMemberCount());
        return true;
    }

}
