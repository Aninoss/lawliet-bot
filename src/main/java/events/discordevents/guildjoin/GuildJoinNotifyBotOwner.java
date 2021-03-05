package events.discordevents.guildjoin;

import core.ShardManager;
import core.MainLogger;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildJoinAbstract;
import org.javacord.api.event.server.ServerJoinEvent;
import org.javacord.api.util.logging.ExceptionLogger;

@DiscordEvent
public class GuildJoinNotifyBotOwner extends GuildJoinAbstract {

    @Override
    public boolean onGuildJoin(ServerJoinEvent event) throws Throwable {
        if (event.getServer().getMemberCount() >= 5000)
            ShardManager.getInstance().fetchOwner().get().sendMessage("**+++** " + StringUtil.escapeMarkdown(event.getServer().getName()) + " (" + event.getServer().getMemberCount() + ")").exceptionally(ExceptionLogger.get());

        MainLogger.get().info("+++ {} ({})", event.getServer().getName(), event.getServer().getMemberCount());
        return true;
    }

}
