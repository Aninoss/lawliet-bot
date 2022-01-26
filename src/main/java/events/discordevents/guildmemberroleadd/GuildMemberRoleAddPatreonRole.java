package events.discordevents.guildmemberroleadd;

import constants.AssetIds;
import constants.Settings;
import core.MainLogger;
import core.cache.PatreonCache;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRoleAddAbstract;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;

@DiscordEvent
public class GuildMemberRoleAddPatreonRole extends GuildMemberRoleAddAbstract {

    @Override
    public boolean onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) throws Throwable {
        if (event.getGuild().getIdLong() == AssetIds.SUPPORT_SERVER_ID) {
            for (long roleId : Settings.PATREON_ROLE_IDS) {
                if (event.getRoles().get(0).getIdLong() == roleId) {
                    MainLogger.get().info("NEW PATREON {} ({})", event.getUser().getAsTag(), event.getUser().getId());
                    JDAUtil.openPrivateChannel(event.getJDA(), AssetIds.OWNER_USER_ID)
                            .flatMap(messageChannel -> messageChannel.sendMessage("NEW PATREON USER: " + StringUtil.escapeMarkdown(event.getUser().getAsTag())))
                            .queue();
                    PatreonCache.getInstance().requestUpdate();
                    break;
                }
            }
        }

        return true;
    }

}
