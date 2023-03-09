package events.discordevents.guildmemberroleremove;

import constants.AssetIds;
import constants.Settings;
import core.MainLogger;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRoleRemoveAbstract;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;

@DiscordEvent
public class GuildMemberRoleRemovePatreonRole extends GuildMemberRoleRemoveAbstract {

    @Override
    public boolean onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) throws Throwable {
        if (event.getGuild().getIdLong() == AssetIds.SUPPORT_SERVER_ID) {
            for (long roleId : Settings.PATREON_ROLE_IDS) {
                if (event.getRoles().get(0).getIdLong() == roleId) {
                    MainLogger.get().info("PATREON LEFT {} ({})", event.getUser().getAsTag(), event.getUser().getId());
                    break;
                }
            }
        }

        return true;
    }

}
