package DiscordListener;

import MySQL.Modules.AutoChannel.DBAutoChannel;
import org.javacord.api.entity.channel.ChannelType;
import org.javacord.api.event.channel.server.ServerChannelDeleteEvent;

public class ServerChannelDeleteListener {

    public void onDelete(ServerChannelDeleteEvent event) throws Exception {
        if (event.getChannel().getType() == ChannelType.SERVER_VOICE_CHANNEL) {
            DBAutoChannel.getInstance()
                    .getBean(event.getServer().getId())
                    .getChildChannels()
                    .removeIf(childChannelId -> childChannelId == null || childChannelId == event.getChannel().getId());
        }
    }

}