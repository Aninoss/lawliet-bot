package events.discordevents.guildmemberremove;

import constants.AssetIds;
import core.MainLogger;
import core.cache.PatreonCache;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRemoveAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;

@DiscordEvent
public class GuildMemberRemovePatreon extends GuildMemberRemoveAbstract {

    @Override
    public boolean onGuildMemberRemove(GuildMemberRemoveEvent event, EntityManagerWrapper entityManager) {
        if (event.getGuild().getIdLong() == AssetIds.SUPPORT_SERVER_ID) {
            if (PatreonCache.getInstance().hasPremium(event.getUser().getIdLong(), false)) {
                MainLogger.get().info("PATREON LEFT (LEFT SERVER) {} ({})", event.getUser().getName(), event.getUser().getId());
            }
        }

        return true;
    }

}
