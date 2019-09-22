package MySQL;

import org.javacord.api.entity.channel.ServerTextChannel;

public class ActivityUserData {

    private int lastPhase = -1, amount;
    private ServerTextChannel channel;

    public ActivityUserData(ServerTextChannel channel) {
        amount = 0;
        this.channel = channel;
    }

    public boolean register(int phase) {
        if (phase > lastPhase) {
            lastPhase = phase;
            amount++;
            return true;
        }

        return false;
    }

    public int getAmount() {
        return amount;
    }

    public ServerTextChannel getChannel() {
        return channel;
    }

}
