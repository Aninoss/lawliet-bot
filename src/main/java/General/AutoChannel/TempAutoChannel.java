package General.AutoChannel;

import General.DiscordApiCollection;
import org.javacord.api.entity.channel.ServerVoiceChannel;

import javax.swing.text.html.Option;
import java.util.Optional;

public class TempAutoChannel {

    private long serverId, tempChannelId, originalChannelId;

    public TempAutoChannel(ServerVoiceChannel originalChannel, ServerVoiceChannel tempChannel) {
        this.serverId = originalChannel.getServer().getId();
        this.originalChannelId = originalChannel.getId();
        this.tempChannelId = tempChannel.getId();
    }

    public long getTempChannelId() {
        return tempChannelId;
    }

    public long getOriginalChannelId() {
        return originalChannelId;
    }

    public Optional<ServerVoiceChannel> getOriginalChannel() {
        return DiscordApiCollection.getInstance().getServerVoiceChannelById(serverId, originalChannelId);
    }

    public long getServerId() {
        return serverId;
    }

}