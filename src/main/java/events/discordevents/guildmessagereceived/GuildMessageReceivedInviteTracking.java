package events.discordevents.guildmessagereceived;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import modules.invitetracking.InviteTracking;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@DiscordEvent(priority = EventPriority.LOW, allowBannedUser = true)
public class GuildMessageReceivedInviteTracking extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(MessageReceivedEvent event, EntityManagerWrapper entityManager) throws Throwable {
        InviteTracking.memberActivity(entityManager.findGuildEntity(event.getGuild().getIdLong()).getInviteTracking(), event.getMember());
        return true;
    }

}
