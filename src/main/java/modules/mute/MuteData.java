package modules.mute;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

public class MuteData {

    private final Server server;
    private final ServerTextChannel channel;
    private final ArrayList<User> users;
    private final Instant stopTime;

    public MuteData(Server server, ServerTextChannel channel, ArrayList<User> users, Instant stopTime) {
        this.server = server;
        this.channel = channel;
        this.users = users;
        this.stopTime = stopTime;
    }

    public MuteData(Server server, ServerTextChannel channel, ArrayList<User> users) {
        this(server, channel, users, null);
    }

    public Server getServer() {
        return server;
    }

    public ServerTextChannel getChannel() {
        return channel;
    }

    public ArrayList<User> getUsers() {
        return users;
    }

    public Optional<Instant> getStopTime() {
        return Optional.ofNullable(stopTime);
    }
}
