package events.discordevents.guildmessagereceived;

import core.MemberCacheController;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.GuildEntity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true, allowBots = true)
public class GuildMessageReceivedSetServerBig extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(MessageReceivedEvent event, EntityManagerWrapper entityManager) throws Throwable {
        Guild guild = event.getGuild();
        boolean isBig = guild.getMemberCount() >= MemberCacheController.BIG_SERVER_THRESHOLD;

        GuildEntity guildEntity = entityManager.findGuildEntity(guild.getIdLong());
        if (isBig != guildEntity.getBig()) {
            guildEntity.beginTransaction();
            guildEntity.setBig(isBig);
            guildEntity.commitTransaction();
        }

        return true;
    }

}
