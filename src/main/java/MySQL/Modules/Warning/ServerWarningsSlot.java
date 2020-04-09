package MySQL.Modules.Warning;

import Core.DiscordApiCollection;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import java.time.Instant;
import java.util.Observable;
import java.util.Optional;

public class ServerWarningsSlot extends Observable {

    private long serverId, userId;
    private Instant time;
    private long requesterUserId;
    private String reason;

    public ServerWarningsSlot(long serverId, long userId, Instant time, long requesterUserId, String reason) {
        this.serverId = serverId;
        this.userId = userId;
        this.time = time;
        this.requesterUserId = requesterUserId;
        this.reason = reason;
    }


    /* Getters */

    public long getServerId() {
        return serverId;
    }

    public Optional<Server> getServer() { return DiscordApiCollection.getInstance().getServerById(serverId); }

    public long getUserId() { return userId; }

    public Optional<User> getUser() { return getServer().flatMap(server -> server.getMemberById(userId)); }

    public Instant getTime() { return time; }

    public long getRequesterUserId() { return requesterUserId; }

    public Optional<User> getRequesterUser() { return getServer().flatMap(server -> server.getMemberById(requesterUserId)); }

    public Optional<String> getReason() { return Optional.ofNullable(reason == null || reason.isEmpty() ? null : reason); }

}