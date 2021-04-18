package modules;

import java.util.List;
import java.util.Locale;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import core.TextManager;
import core.atomicassets.AtomicMember;
import core.utils.StringUtil;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.entities.Member;

public class FisheryMemberGroup {

    private final long guildId;
    private final List<AtomicMember> members;

    public FisheryMemberGroup(long guildId, List<Member> members) {
        this.guildId = guildId;
        this.members = members.stream()
                .map(AtomicMember::new)
                .collect(Collectors.toList());
    }

    public String getAsTag() {
        AtomicMember atomicMember = members.get(0);
        if (members.size() == 1 && atomicMember.get().isPresent()) {
            return atomicMember.get().get().getUser().getAsTag();
        } else {
            Locale locale = DBGuild.getInstance().retrieve(guildId).getLocale();
            return StringUtil.numToString(members.size()) + " " + TextManager.getString(locale, TextManager.GENERAL, "members", members.size() != 1);
        }
    }

    public List<FisheryMemberData> getFisheryMemberList() {
        return members.stream()
                .map(m -> DBFishery.getInstance().retrieve(guildId).getMemberBean(m.getIdLong()))
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

}
