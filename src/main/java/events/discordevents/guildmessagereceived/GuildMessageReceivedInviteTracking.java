package events.discordevents.guildmessagereceived;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import modules.invitetracking.InviteTracking;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@DiscordEvent(priority = EventPriority.LOW, allowBannedUser = true)
public class GuildMessageReceivedInviteTracking extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(GuildMessageReceivedEvent event) throws Throwable {
        InviteTracking.memberActivity(event.getMember());
        return true;
    }

}
