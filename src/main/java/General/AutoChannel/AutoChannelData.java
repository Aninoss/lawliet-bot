package General.AutoChannel;

import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.server.Server;

public class AutoChannelData {
    private Server server;
    private ServerVoiceChannel voiceChannel;
    private boolean active, creatorCanDisconnect;
    private String channelName;

    public AutoChannelData(Server server, ServerVoiceChannel voiceChannel, boolean active, String channelName, boolean creatorCanDisconnect) {
        this.server = server;
        this.voiceChannel = voiceChannel;
        this.active = active;
        this.channelName = channelName;
        this.creatorCanDisconnect = creatorCanDisconnect;
    }

    public Server getServer() {
        return server;
    }

    public ServerVoiceChannel getVoiceChannel() {
        return voiceChannel;
    }

    public boolean isActive() {
        return active;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setVoiceChannel(ServerVoiceChannel voiceChannel) {
        this.voiceChannel = voiceChannel;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public boolean isCreatorCanDisconnect() {
        return creatorCanDisconnect;
    }

    public void setCreatorCanDisconnect(boolean creatorCanDisconnect) {
        this.creatorCanDisconnect = creatorCanDisconnect;
    }
}
