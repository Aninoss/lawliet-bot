package events.discordevents.guildmessagereceived;

import core.MemberCacheController;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true, allowBots = true)
public class GuildMessageReceivedSetServerBig extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(MessageReceivedEvent event) throws Throwable {
        Guild guild = event.getGuild();
        DBGuild.getInstance().retrieve(guild.getIdLong())
                .setBig(guild.getMemberCount() >= MemberCacheController.BIG_SERVER_THRESHOLD);
        return true;
    }

}
