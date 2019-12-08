package MySQL;

import org.javacord.api.entity.channel.ServerTextChannel;

import java.util.Optional;

public class ActivityUserData {

    private int lastPhase = -1, message, vc;
    private ServerTextChannel channel = null;

    public ActivityUserData() {
        message = 0;
        vc = 0;
    }

    public boolean registerMessage(int phase, ServerTextChannel channel) {
        if (phase > lastPhase) {
            lastPhase = phase;
            message++;
            if (channel != null) this.channel = channel;
            return true;
        }

        return false;
    }

    public void registerVC() {
        vc++;
    }

    public int getAmountMessage() {
        return message;
    }

    public int getAmountVC() {
        return vc;
    }

    public Optional<ServerTextChannel> getChannel() {
        return Optional.ofNullable(channel);
    }

    public void reset() {
        message = 0;
        vc = 0;
    }

}
