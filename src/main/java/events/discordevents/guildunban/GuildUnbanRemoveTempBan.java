package events.discordevents.guildunban;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildUnbanAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.modules.tempban.DBTempBan;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildUnbanRemoveTempBan extends GuildUnbanAbstract {

    @Override
    public boolean onGuildUnban(GuildUnbanEvent event, EntityManagerWrapper entityManager) throws Throwable {
        DBTempBan.getInstance().retrieve(event.getGuild().getIdLong()).remove(event.getUser().getIdLong());
        return true;
    }

}
