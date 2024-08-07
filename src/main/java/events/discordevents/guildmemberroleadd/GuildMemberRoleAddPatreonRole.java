package events.discordevents.guildmemberroleadd;

import constants.AssetIds;
import constants.Settings;
import core.MainLogger;
import core.cache.PatreonCache;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRoleAddAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;

@DiscordEvent
public class GuildMemberRoleAddPatreonRole extends GuildMemberRoleAddAbstract {

    @Override
    public boolean onGuildMemberRoleAdd(GuildMemberRoleAddEvent event, EntityManagerWrapper entityManager) throws Throwable {
        if (event.getGuild().getIdLong() == AssetIds.SUPPORT_SERVER_ID) {
            for (long roleId : Settings.PATREON_ROLE_IDS) {
                if (event.getRoles().get(0).getIdLong() == roleId) {
                    MainLogger.get().info("NEW PATREON {} ({})", event.getUser().getName(), event.getUser().getId());
                    PatreonCache.getInstance().requestUpdate();
                    break;
                }
            }
        }

        return true;
    }

}
