package mysql.modules.warning;

import java.time.Instant;
import java.util.Optional;
import core.MemberCacheController;
import core.assets.MemberAsset;
import mysql.DataWithGuild;
import net.dv8tion.jda.api.entities.Member;

public class ServerWarningSlot extends DataWithGuild implements MemberAsset {

    private final long memberId;
    private final Instant time;
    private final long requesterUserId;
    private final String reason;

    public ServerWarningSlot(long serverId, long memberId, Instant time, long requesterUserId, String reason) {
        super(serverId);
        this.memberId = memberId;
        this.time = time;
        this.requesterUserId = requesterUserId;
        this.reason = reason;
    }

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
        return getGuild().map(guild -> {
            MemberCacheController.getInstance().loadMembers(guild).join();
            return guild.getMemberById(requesterUserId);
        });
    }

    public Optional<String> getReason() {
        return Optional.ofNullable(reason == null || reason.isEmpty() ? null : reason);
    }

}