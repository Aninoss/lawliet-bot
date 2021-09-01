package mysql.modules.membercountdisplays;

import java.util.Map;
import core.CustomObservableMap;
import mysql.DataWithGuild;

public class MemberCountData extends DataWithGuild {

    private final CustomObservableMap<Long, MemberCountDisplaySlot> memberCountBeanSlots;

    public MemberCountData(long serverId, Map<Long, MemberCountDisplaySlot> memberCountBeanSlots) {
        super(serverId);
        this.memberCountBeanSlots = new CustomObservableMap<>(memberCountBeanSlots);
    }

    public CustomObservableMap<Long, MemberCountDisplaySlot> getMemberCountBeanSlots() {
        return memberCountBeanSlots;
    }

}
