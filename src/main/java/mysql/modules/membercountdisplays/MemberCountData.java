package mysql.modules.membercountdisplays;

import core.CustomObservableMap;
import mysql.DataWithGuild;

import java.util.Map;

public class MemberCountData extends DataWithGuild {

    private final CustomObservableMap<Long, MemberCountDisplaySlot> memberCountDisplaySlots;

    public MemberCountData(long serverId, Map<Long, MemberCountDisplaySlot> memberCountDisplaySlots) {
        super(serverId);
        this.memberCountDisplaySlots = new CustomObservableMap<>(memberCountDisplaySlots);
    }

    public CustomObservableMap<Long, MemberCountDisplaySlot> getMemberCountDisplaySlots() {
        return memberCountDisplaySlots;
    }

}
