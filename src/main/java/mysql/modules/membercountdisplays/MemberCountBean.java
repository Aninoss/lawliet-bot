package mysql.modules.membercountdisplays;

import core.CustomObservableMap;
import mysql.BeanWithServer;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;

public class MemberCountBean extends BeanWithServer {

    private final CustomObservableMap<Long, MemberCountDisplaySlot> memberCountBeanSlots;

    public MemberCountBean(long serverId, @NonNull HashMap<Long, MemberCountDisplaySlot> memberCountBeanSlots) {
        super(serverId);
        this.memberCountBeanSlots = new CustomObservableMap<>(memberCountBeanSlots);
    }


    /* Getters */

    public CustomObservableMap<Long, MemberCountDisplaySlot> getMemberCountBeanSlots() { return memberCountBeanSlots; }

}
