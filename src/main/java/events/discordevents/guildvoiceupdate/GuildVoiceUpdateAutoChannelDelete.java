package events.discordevents.guildvoiceupdate;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildVoiceUpdateAbstract;
import modules.AutoChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;

@DiscordEvent
public class GuildVoiceUpdateAutoChannelDelete extends GuildVoiceUpdateAbstract {

    @Override
    public boolean onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getChannelLeft() != null && event.getChannelLeft() instanceof VoiceChannel) {
            AutoChannel.processRemove((VoiceChannel) event.getChannelLeft());
        }
        return true;
    }

}
