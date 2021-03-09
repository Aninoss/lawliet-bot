package events.discordevents.guildvoicemove;

import events.discordevents.eventtypeabstracts.GuildVoiceMoveAbstract;
import modules.AutoChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;

public class GuildVoiceMoveAutoChannel extends GuildVoiceMoveAbstract {

    @Override
    public boolean onGuildVoiceMove(GuildVoiceMoveEvent event) {
        AutoChannel.processCreate(event.getChannelJoined(), event.getMember());
        AutoChannel.processRemove(event.getChannelLeft());
        return true;
    }

}
