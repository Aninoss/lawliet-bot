package General;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;

import java.util.Optional;

public class ModerationStatus {

    private Server server;
    private ServerTextChannel channel;
    private boolean question;
    private int autoKick, autoBan, autoKickDays, autoBanDays;

    public ModerationStatus(Server server, ServerTextChannel channel, boolean question, int autoKick, int autoBan, int autoKickDays, int autoBanDays) {
        this.server = server;
        this.channel = channel;
        this.question = question;
        this.autoKick = autoKick;
        this.autoBan = autoBan;
        this.autoKickDays = autoKickDays;
        this.autoBanDays = autoBanDays;
    }

    public Server getServer() {
        return server;
    }

    public Optional<ServerTextChannel> getChannel() {
        return Optional.ofNullable(channel);
    }

    public int getAutoKick() {
        return autoKick;
    }

    public int getAutoBan() {
        return autoBan;
    }

    public void setAutoKick(int autoKick) {
        this.autoKick = autoKick;
    }

    public void setAutoBan(int autoBan) {
        this.autoBan = autoBan;
    }

    public int getAutoKickDays() { return autoKickDays; }

    public void setAutoKickDays(int autoKickDays) { this.autoKickDays = autoKickDays; }

    public int getAutoBanDays() { return autoBanDays; }

    public void setAutoBanDays(int autoBanDays) { this.autoBanDays = autoBanDays; }

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
