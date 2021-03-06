package core.atomicassets;

import core.ShardManager;
import net.dv8tion.jda.api.entities.Member;

import java.util.Objects;
import java.util.Optional;

public class AtomicMember implements AtomicAsset<Member> {

    private final long guildId;
    private final long memberId;

    public AtomicMember(long guildId, long memberId) {
        this.guildId = guildId;
        this.memberId = memberId;
    }

    public AtomicMember(Member member) {
        guildId = member.getGuild().getIdLong();
        memberId = member.getIdLong();
    }

    @Override
    public long getId() {
        return memberId;
    }

    @Override
    public Optional<Member> get() {
        return ShardManager.getInstance().getLocalGuildById(guildId)
                .flatMap(guild -> Optional.ofNullable(guild.getMemberById(memberId)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtomicMember that = (AtomicMember) o;
        return memberId == that.memberId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId);
    }

}
