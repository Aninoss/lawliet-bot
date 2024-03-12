package modules.fishery;

import core.TextManager;
import core.assets.GuildAsset;
import core.atomicassets.AtomicMember;
import core.utils.StringUtil;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.entities.Member;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

public class FisheryMemberGroup implements GuildAsset {

    private final long guildId;
    private final List<AtomicMember> members;

    public FisheryMemberGroup(long guildId, List<?> members) {
        this.guildId = guildId;
        if (members.isEmpty()) {
            this.members = Collections.emptyList();
        } else if (members.get(0) instanceof Member) {
            this.members = members.stream()
                    .map(m -> new AtomicMember((Member) m))
                    .collect(Collectors.toList());
        } else if (members.get(0) instanceof FisheryMemberData) {
            this.members = members.stream()
                    .map(m -> {
                        FisheryMemberData fm = (FisheryMemberData) m;
                        return new AtomicMember(fm.getGuildId(), fm.getMemberId());
                    })
                    .collect(Collectors.toList());
        } else if (members.get(0) instanceof AtomicMember) {
            this.members = (List<AtomicMember>) members;
        } else {
            this.members = Collections.emptyList();
        }
    }

    public String getUsernames(Locale locale) {
        AtomicMember atomicMember = members.get(0);
        if (members.size() == 1 && atomicMember.get().isPresent()) {
            return StringUtil.escapeMarkdown(atomicMember.get().get().getUser().getName());
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
