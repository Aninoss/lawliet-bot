package events.discordevents.guildvoiceupdate;

import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildVoiceUpdateAbstract;
import modules.invitetracking.InviteTracking;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;

@DiscordEvent(priority = EventPriority.LOWEST, allowBannedUser = true)
public class GuildVoiceUpdateInviteTracking extends GuildVoiceUpdateAbstract {

    @Override
    public boolean onGuildVoiceUpdate(GuildVoiceUpdateEvent event, EntityManagerWrapper entityManager) {
        InviteTracking.memberActivity(event.getMember());
        return true;
    }

}
