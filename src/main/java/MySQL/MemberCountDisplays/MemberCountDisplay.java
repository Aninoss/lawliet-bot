package MySQL.MemberCountDisplays;

import General.CustomObservableList;
import General.DiscordApiCollection;
import MySQL.Server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.server.Server;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Optional;

public class MemberCountDisplay {

    private long serverId, voiceChannelId;
    private String mask;

    public MemberCountDisplay(long serverId, long voiceChannelId, String mask) {
        this.serverId = serverId;
        this.voiceChannelId = voiceChannelId;
        this.mask = mask;
    }

    public long getVoiceChannelId() {
        return voiceChannelId;
    }

    public Optional<ServerVoiceChannel> getVoiceChannel() {
        return DiscordApiCollection.getInstance().getServerById(serverId).flatMap(server -> server.getVoiceChannelById(voiceChannelId));
    }

    public String getMask() {
        return mask;
    }

}
