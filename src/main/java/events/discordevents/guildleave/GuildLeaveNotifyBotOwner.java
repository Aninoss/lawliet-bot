package events.discordevents.guildleave;

import core.ShardManager;
import core.MainLogger;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildLeaveAbstract;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;

@DiscordEvent
public class GuildLeaveNotifyBotOwner extends GuildLeaveAbstract {

    @Override
    public boolean onGuildLeave(GuildLeaveEvent event) throws Throwable {
        if (event.getGuild().getMemberCount() >= 5000) {
            JDAUtil.sendPrivateMessage(ShardManager.getInstance().fetchOwner().get(), "**---** " + StringUtil.escapeMarkdown(event.getGuild().getName()) + " (" + event.getGuild().getMemberCount() + ")")
                    .queue();
        }

        MainLogger.get().info("--- {} ({})", event.getGuild().getName(), event.getGuild().getMemberCount());
        return true;
    }

}
