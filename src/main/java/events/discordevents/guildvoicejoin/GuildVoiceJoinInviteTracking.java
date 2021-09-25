package events.discordevents.guildvoicejoin;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildVoiceJoinAbstract;
import modules.invitetracking.InviteTracking;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;

@DiscordEvent(allowBannedUser = true)
public class GuildVoiceJoinInviteTracking extends GuildVoiceJoinAbstract {

    @Override
    public boolean onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        InviteTracking.memberActivity(event.getMember());
        return true;
    }

}
