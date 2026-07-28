package events.discordevents.guildmemberjoin;

import constants.AssetIds;
import core.MainLogger;
import core.patreon.PatreonCache;
import core.patreon.PatreonTier;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

@DiscordEvent
public class GuildMemberJoinPatreon extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event, EntityManagerWrapper entityManager) throws Throwable {
        if (event.getGuild().getIdLong() == AssetIds.SUPPORT_SERVER_ID) {
            for (PatreonTier patreonTier : PatreonTier.values()) {
                if (event.getMember().getRoles().stream().anyMatch(role -> role.getIdLong() == patreonTier.getRoleId())) {
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
