package events.discordevents.guildvoiceupdate;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildVoiceUpdateAbstract;
import modules.invitetracking.InviteTracking;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;

@DiscordEvent(allowBannedUser = true)
public class GuildVoiceUpdateInviteTracking extends GuildVoiceUpdateAbstract {

    @Override
    public boolean onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        InviteTracking.memberActivity(event.getMember());
        return true;
    }

}
