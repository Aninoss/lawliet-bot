package events.discordevents.guildmemberroleadd;

import constants.AssetIds;
import core.MainLogger;
import core.patreon.PatreonCache;
import core.patreon.PatreonTier;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRoleAddAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;

@DiscordEvent
public class GuildMemberRoleAddPatreonRole extends GuildMemberRoleAddAbstract {

    @Override
    public boolean onGuildMemberRoleAdd(GuildMemberRoleAddEvent event, EntityManagerWrapper entityManager) throws Throwable {
        if (event.getGuild().getIdLong() == AssetIds.SUPPORT_SERVER_ID) {
            for (PatreonTier patreonTier : PatreonTier.values()) {
                if (event.getRoles().get(0).getIdLong() == patreonTier.getRoleId()) {
                    MainLogger.get().info("NEW PATREON {} ({})", event.getUser().getName(), event.getUser().getId());
                    PatreonCache.sendJoinDm(event.getUser().getIdLong(), patreonTier);
                    PatreonCache.getInstance().requestUpdate();
                    break;
                }
            }
        }

        return true;
    }

}
