package events.discordevents.guildleave;

import constants.AssetIds;
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
            JDAUtil.sendPrivateMessage(
                    AssetIds.OWNER_USER_ID,
                    "**---** " + StringUtil.escapeMarkdown(event.getGuild().getName()) + " (" + event.getGuild().getMemberCount() + ")"
            ).queue();
        }

        MainLogger.get().info("--- {} ({}; {} members)", event.getGuild().getName(), event.getGuild().getIdLong(), event.getGuild().getMemberCount());
        return true;
    }

}
