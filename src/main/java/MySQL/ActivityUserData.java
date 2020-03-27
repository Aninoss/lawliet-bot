package MySQL;

import General.DiscordApiCollection;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;

import java.util.Optional;

public class ActivityUserData {

    private int lastPhase = -1, message, vc;
    private long serverId = -1, channelId = -1;

    public ActivityUserData() {
        message = 0;
        vc = 0;
    }

    public boolean registerMessage(int phase, ServerTextChannel channel) {
        if (phase > lastPhase) {
            lastPhase = phase;
            message++;
            if (channel != null) {
                this.channelId = channel.getId();
                this.serverId = channel.getServer().getId();
            }
            return true;
        }

        return false;
    }

    public void registerVC(int amount) {
        vc += amount;
    }

    public int getAmountMessage() {
        return message;
    }

    public int getAmountVC() {
        return vc;
    }

    public Optional<ServerTextChannel> getChannel() {
        return DiscordApiCollection.getInstance().getServerTextChannelById(serverId, channelId);
    }

    public void reset() {
        message = 0;
        vc = 0;
    }

}
