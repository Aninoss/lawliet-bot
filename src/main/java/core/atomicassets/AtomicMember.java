package core.atomicassets;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import core.CustomObservableList;
import core.MemberCacheController;
import core.ShardManager;
import core.TextManager;
import core.utils.StringUtil;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.entities.Guild;
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
        return ShardManager.getLocalGuildById(guildId)
                .map(guild -> MemberCacheController.getInstance().loadMember(guild, memberId).join());
    }

    @Override
    public Locale getLocale() {
        return DBGuild.getInstance().retrieve(guildId).getLocale();
    }

    @Override
    public Optional<String> getPrefixedNameRaw() {
        return get().map(m -> "@" + m.getEffectiveName());
    }

    @Override
    public Optional<String> getNameRaw() {
        return get().map(Member::getEffectiveName);
    }

    public Optional<String> getTaggedNameRaw() {
        return get().map(member -> member.getUser().getAsTag());
    }

    public String getTaggedName() {
        return getTaggedNameRaw()
                .orElseGet(() -> TextManager.getString(getLocale(), TextManager.GENERAL, "notfound", StringUtil.numToHex(getIdLong())));
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
                AtomicMember::getIdLong
        );
    }

}
