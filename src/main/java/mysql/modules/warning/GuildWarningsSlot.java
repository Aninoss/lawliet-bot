package mysql.modules.warning;

import core.assets.MemberAsset;
import mysql.BeanWithGuild;
import net.dv8tion.jda.api.entities.Member;
import java.time.Instant;
import java.util.Optional;

public class GuildWarningsSlot extends BeanWithGuild implements MemberAsset {

    private final long memberId;
    private final Instant time;
    private final long requesterUserId;
    private final String reason;

    public GuildWarningsSlot(long serverId, long memberId, Instant time, long requesterUserId, String reason) {
        super(serverId);
        this.memberId = memberId;
        this.time = time;
        this.requesterUserId = requesterUserId;
        this.reason = reason;
    }


    /* Getters */

    @Override
    public long getMemberId() {
        return memberId;
    }

    public Instant getTime() {
        return time;
    }

    public long getRequesterUserId() {
        return requesterUserId;
    }

    public Optional<Member> getRequesterMember() {
        return getGuild().map(guild -> guild.getMemberById(requesterUserId));
    }

    public Optional<String> getReason() {
        return Optional.ofNullable(reason == null || reason.isEmpty() ? null : reason);
    }

}