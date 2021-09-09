package events.discordevents.guildmemberjoin;

import constants.AssetIds;
import constants.Settings;
import core.MainLogger;
import core.cache.PatreonCache;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

@DiscordEvent
public class GuildMemberJoinPatreon extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event) throws Throwable {
        if (event.getGuild().getIdLong() == AssetIds.SUPPORT_SERVER_ID) {
            for (long roleId : Settings.PATREON_ROLE_IDS) {
                if (event.getMember().getRoles().stream().anyMatch(role -> role.getIdLong() == roleId)) {
                    MainLogger.get().info("NEW PATREON {} ({})", event.getUser().getAsTag(), event.getUser().getId());
                    JDAUtil.sendPrivateMessage(
                            AssetIds.OWNER_USER_ID,
                            "NEW PATREON USER: " + StringUtil.escapeMarkdown(event.getUser().getAsTag())
                    ).queue();
                    PatreonCache.getInstance().requestUpdate();
                    break;
                }
            }
        }

        return true;
    }

}
