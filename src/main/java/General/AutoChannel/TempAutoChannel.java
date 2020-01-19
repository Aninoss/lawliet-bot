package General.AutoChannel;

import General.DiscordApiCollection;
import org.javacord.api.entity.channel.ServerVoiceChannel;

public class TempAutoChannel {

    private long serverId, tempChannelId, originalChannelId;

    public TempAutoChannel(ServerVoiceChannel originalChannel, ServerVoiceChannel tempChannel) {
        this.serverId = originalChannel.getServer().getId();
        this.originalChannelId = originalChannel.getId();
        this.tempChannelId = tempChannel.getId();
    }

    public ServerVoiceChannel getTempChannel() {
        return DiscordApiCollection.getInstance().getServerById(serverId).get().getVoiceChannelById(tempChannelId).get();
    }

    public ServerVoiceChannel getOriginalChannel() {
        return DiscordApiCollection.getInstance().getServerById(serverId).get().getVoiceChannelById(originalChannelId).get();
    }
}