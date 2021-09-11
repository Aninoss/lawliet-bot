package events.discordevents.guildvoicemove;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildVoiceMoveAbstract;
import modules.invitetracking.InviteTracking;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;

@DiscordEvent
public class GuildVoiceMoveInviteTracking extends GuildVoiceMoveAbstract {

    @Override
    public boolean onGuildVoiceMove(GuildVoiceMoveEvent event) {
        InviteTracking.memberActivity(event.getMember());
        return true;
    }

}
