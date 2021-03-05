package events.discordevents.guildleave;

import core.ShardManager;
import core.MainLogger;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildLeaveAbstract;
import org.javacord.api.event.server.ServerLeaveEvent;
import org.javacord.api.util.logging.ExceptionLogger;

@DiscordEvent
public class GuildLeaveNotifyBotOwner extends GuildLeaveAbstract {

    @Override
    public boolean onGuildLeave(ServerLeaveEvent event) throws Throwable {
        if (event.getServer().getMemberCount() >= 5000)
            ShardManager.getInstance().fetchOwner().get().sendMessage("**---** " + StringUtil.escapeMarkdown(event.getServer().getName()) + " (" + event.getServer().getMemberCount() + ")").exceptionally(ExceptionLogger.get());

        MainLogger.get().info("--- {} ({})", event.getServer().getName(), event.getServer().getMemberCount());
        return true;
    }

}
