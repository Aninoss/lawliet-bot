package DiscordListener;

import General.AutoChannel.AutoChannelContainer;
import org.javacord.api.entity.channel.ChannelType;
import org.javacord.api.event.channel.server.ServerChannelDeleteEvent;

public class ServerChannelDeleteListener {

    public void onDelete(ServerChannelDeleteEvent event) {
        if (event.getChannel().getType() == ChannelType.SERVER_VOICE_CHANNEL) AutoChannelContainer.getInstance().removeVoiceChannel(event.getChannel().getId());
    }
}