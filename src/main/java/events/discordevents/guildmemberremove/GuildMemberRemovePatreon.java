package events.discordevents.guildmemberremove;

import constants.AssetIds;
import core.MainLogger;
import core.cache.PatreonCache;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRemoveAbstract;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;

@DiscordEvent
public class GuildMemberRemovePatreon extends GuildMemberRemoveAbstract {

    @Override
    public boolean onGuildMemberRemove(GuildMemberRemoveEvent event) {
        if (event.getGuild().getIdLong() == AssetIds.SUPPORT_SERVER_ID) {
            if (PatreonCache.getInstance().getUserTier(event.getUser().getIdLong(), false) > 0) {
                MainLogger.get().info("PATREON LEFT (LEFT SERVER) {} ({})", event.getUser().getAsTag(), event.getUser().getId());
                JDAUtil.sendPrivateMessage(event.getMember(), "PATREON USER LEFT (LEFT SERVER): " + StringUtil.escapeMarkdown(event.getUser().getAsTag()))
                        .queue();
            }
        }

        return true;
    }

}
