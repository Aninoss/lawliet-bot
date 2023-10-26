package modules.fishery;

import java.util.List;
import java.util.Locale;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import core.TextManager;
import core.assets.GuildAsset;
import core.atomicassets.AtomicMember;
import core.utils.StringUtil;
import mysql.redis.fisheryusers.FisheryUserManager;
import mysql.redis.fisheryusers.FisheryMemberData;
import net.dv8tion.jda.api.entities.Member;

public class FisheryMemberGroup implements GuildAsset {

    private final long guildId;
    private final List<AtomicMember> members;

    public FisheryMemberGroup(long guildId, List<Member> members) {
        this.guildId = guildId;
        this.members = members.stream()
                .map(AtomicMember::new)
                .collect(Collectors.toList());
    }

    public String getAsTag(Locale locale) {
        AtomicMember atomicMember = members.get(0);
        if (members.size() == 1 && atomicMember.get().isPresent()) {
            return StringUtil.escapeMarkdown(atomicMember.get().get().getUser().getAsTag());
        } else {
            return StringUtil.numToString(members.size()) + " " + TextManager.getString(locale, TextManager.GENERAL, "members", members.size() != 1);
        }
    }

    public List<FisheryMemberData> getFisheryMemberList() {
        return members.stream()
                .map(m -> FisheryUserManager.getGuildData(guildId).getMemberData(m.getIdLong()))
                .collect(Collectors.toList());
    }

    public String getFishString() {
        return getValueString(FisheryMemberData::getFish);
    }

    public String getCoinsString() {
        return getValueString(FisheryMemberData::getCoins);
    }

    public String getDailyStreakString() {
        return getValueString(FisheryMemberData::getDailyStreak);
    }

    public String getGearString(FisheryGear gear) {
        return getValueString(fisheryMemberData -> fisheryMemberData.getMemberGear(gear).getLevel());
    }

    public boolean containsMultiple() {
        return members.size() != 1;
    }

    private String getValueString(ToLongFunction<? super FisheryMemberData> mapper) {
        long min = getFisheryMemberList().stream()
                .mapToLong(mapper)
                .min()
                .orElse(0);

        long max = getFisheryMemberList().stream()
                .mapToLong(mapper)
                .max()
                .orElse(0);

        if (min == max) {
            return StringUtil.numToString(min);
        } else {
            return StringUtil.numToString(min) + " - " + StringUtil.numToString(max);
        }
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

}
