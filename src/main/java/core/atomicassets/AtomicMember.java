package core.atomicassets;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import core.CustomObservableList;
import core.ShardManager;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;

public class AtomicMember implements MentionableAtomicAsset<Member> {

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
    public long getIdLong() {
        return memberId;
    }

    @Override
    public Optional<Member> get() {
        return ShardManager.getInstance().getLocalGuildById(guildId)
                .map(guild -> guild.getMemberById(memberId));
    }

    @Override
    public Locale getLocale() {
        return DBGuild.getInstance().retrieve(guildId).getLocale();
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

    public static List<AtomicMember> from(List<Member> members) {
        return members.stream()
                .map(AtomicMember::new)
                .collect(Collectors.toList());
    }

    public static List<Member> to(List<AtomicMember> members) {
        return members.stream()
                .map(AtomicMember::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static CustomObservableList<AtomicMember> transformIdList(Guild guild, CustomObservableList<Long> list) {
        return list.transform(
                id -> new AtomicMember(guild.getIdLong(), id),
                atomic -> atomic.get().map(ISnowflake::getIdLong).orElse(null)
        );
    }

}
