package events.discordevents.guildvoicejoin;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildVoiceJoinAbstract;
import modules.AutoChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;

@DiscordEvent
public class GuildVoiceJoinAutoChannel extends GuildVoiceJoinAbstract {

    @Override
    public boolean onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        AutoChannel.processCreate((VoiceChannel) event.getChannelJoined(), event.getMember());
        return true;
    }

}
