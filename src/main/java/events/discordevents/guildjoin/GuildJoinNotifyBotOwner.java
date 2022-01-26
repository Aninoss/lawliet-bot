package events.discordevents.guildjoin;

import constants.AssetIds;
import core.MainLogger;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildJoinAbstract;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;

@DiscordEvent
public class GuildJoinNotifyBotOwner extends GuildJoinAbstract {

    @Override
    public boolean onGuildJoin(GuildJoinEvent event) {
        if (event.getGuild().getMemberCount() >= 5000) {
            JDAUtil.openPrivateChannel(event.getJDA(), AssetIds.OWNER_USER_ID)
                    .flatMap(messageChannel -> messageChannel.sendMessage("**+++** " + StringUtil.escapeMarkdown(event.getGuild().getName()) + " (" + event.getGuild().getMemberCount() + ")"))
                    .queue();
        }

        MainLogger.get().info("+++ {} ({}; {} members)", event.getGuild().getName(), event.getGuild().getIdLong(), event.getGuild().getMemberCount());
        return true;
    }

}
