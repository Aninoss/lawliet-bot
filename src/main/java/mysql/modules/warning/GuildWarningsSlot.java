package mysql.modules.warning;

import mysql.BeanWithGuild;
import org.javacord.api.entity.user.User;

import java.time.Instant;
import java.util.Optional;

public class GuildWarningsSlot extends BeanWithGuild {

    private final long userId;
    private final Instant time;
    private final long requesterUserId;
    private final String reason;

    public GuildWarningsSlot(long serverId, long userId, Instant time, long requesterUserId, String reason) {
        super(serverId);
        this.userId = userId;
        this.time = time;
        this.requesterUserId = requesterUserId;
        this.reason = reason;
    }


    /* Getters */

    public long getUserId() { return userId; }

    public Optional<User> getUser() { return getGuild().flatMap(server -> server.getMemberById(userId)); }

    public Instant getTime() { return time; }

    public long getRequesterUserId() { return requesterUserId; }

    public Optional<User> getRequesterUser() { return getGuild().flatMap(server -> server.getMemberById(requesterUserId)); }

    public Optional<String> getReason() { return Optional.ofNullable(reason == null || reason.isEmpty() ? null : reason); }

}