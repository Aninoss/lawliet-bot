package events.discordevents.guildvoicemove;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildVoiceMoveAbstract;
import modules.AutoChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;

@DiscordEvent(allowBots = true)
public class GuildVoiceMoveAutoChannelCreate extends GuildVoiceMoveAbstract {

    @Override
    public boolean onGuildVoiceMove(GuildVoiceMoveEvent event) {
        AutoChannel.processCreate(event.getChannelJoined(), event.getMember());
        return true;
    }

}
