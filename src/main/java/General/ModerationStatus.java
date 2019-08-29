package General;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;

public class ModerationStatus {
    private Server server;
    private ServerTextChannel channel;
    private boolean question;

    public ModerationStatus(Server server, ServerTextChannel channel, boolean question) {
        this.server = server;
        this.channel = channel;
        this.question = question;
    }

    public Server getServer() {
        return server;
    }

    public ServerTextChannel getChannel() {
        return channel;
    }

    public boolean isQuestion() {
        return question;
    }

    public void setChannel(ServerTextChannel channel) {
        this.channel = channel;
    }

    public void switchQuestion() {
        this.question = !this.question;
    }
}
