package mysql.modules.membercountdisplays;

import java.util.HashMap;
import core.CustomObservableMap;
import mysql.BeanWithGuild;
import org.checkerframework.checker.nullness.qual.NonNull;

public class MemberCountBean extends BeanWithGuild {

    private final CustomObservableMap<Long, MemberCountDisplaySlot> memberCountBeanSlots;

    public MemberCountBean(long serverId, @NonNull HashMap<Long, MemberCountDisplaySlot> memberCountBeanSlots) {
        super(serverId);
        this.memberCountBeanSlots = new CustomObservableMap<>(memberCountBeanSlots);
    }


    /* Getters */

    public CustomObservableMap<Long, MemberCountDisplaySlot> getMemberCountBeanSlots() { return memberCountBeanSlots; }

}
