package DiscordListener.ServerChannelDelete;

import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerTypeAbstracts.ReactionAddAbstract;
import DiscordListener.ListenerTypeAbstracts.ServerChannelDeleteAbstract;
import Modules.ReactionCommandCheck;
import MySQL.Modules.AutoChannel.DBAutoChannel;
import org.javacord.api.entity.channel.ChannelType;
import org.javacord.api.event.channel.server.ServerChannelDeleteEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;

@DiscordListenerAnnotation()
public class ServerChannelDeleteAutoChannel extends ServerChannelDeleteAbstract {

    @Override
    public boolean onServerChannelDelete(ServerChannelDeleteEvent event) throws Throwable {
        if (event.getChannel().getType() == ChannelType.SERVER_VOICE_CHANNEL) {
            DBAutoChannel.getInstance()
                    .getBean(event.getServer().getId())
                    .getChildChannels()
                    .removeIf(childChannelId -> childChannelId == null || childChannelId == event.getChannel().getId());
        }

        return true;
    }

}
