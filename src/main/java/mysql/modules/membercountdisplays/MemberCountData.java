package mysql.modules.membercountdisplays;

import java.util.HashMap;
import core.CustomObservableMap;
import mysql.DataWithGuild;
import org.checkerframework.checker.nullness.qual.NonNull;

public class MemberCountData extends DataWithGuild {

    private final CustomObservableMap<Long, MemberCountDisplaySlot> memberCountBeanSlots;

    public MemberCountData(long serverId, @NonNull HashMap<Long, MemberCountDisplaySlot> memberCountBeanSlots) {
        super(serverId);
        this.memberCountBeanSlots = new CustomObservableMap<>(memberCountBeanSlots);
    }

    public CustomObservableMap<Long, MemberCountDisplaySlot> getMemberCountBeanSlots() {
        return memberCountBeanSlots;
    }

}
