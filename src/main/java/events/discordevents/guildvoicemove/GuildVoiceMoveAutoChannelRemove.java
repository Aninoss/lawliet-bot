package events.discordevents.guildvoicemove;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildVoiceMoveAbstract;
import modules.AutoChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildVoiceMoveAutoChannelRemove extends GuildVoiceMoveAbstract {

    @Override
    public boolean onGuildVoiceMove(GuildVoiceMoveEvent event) {
        AutoChannel.processRemove((VoiceChannel) event.getChannelLeft());
        return true;
    }

}
