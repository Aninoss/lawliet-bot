package events.discordevents.guildvoiceleave;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildVoiceLeaveAbstract;
import modules.AutoChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildVoiceLeaveAutoChannel extends GuildVoiceLeaveAbstract {

    @Override
    public boolean onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        AutoChannel.processRemove((VoiceChannel) event.getChannelLeft());
        return true;
    }

}
