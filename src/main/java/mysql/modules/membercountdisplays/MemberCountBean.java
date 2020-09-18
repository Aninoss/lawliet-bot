package mysql.modules.membercountdisplays;

import core.CustomObservableMap;
import mysql.BeanWithServer;
import mysql.modules.server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;

public class MemberCountBean extends BeanWithServer {

    private final CustomObservableMap<Long, MemberCountDisplay> memberCountBeanSlots;

    public MemberCountBean(ServerBean serverBean, @NonNull HashMap<Long, MemberCountDisplay> memberCountBeanSlots) {
        super(serverBean);
        this.memberCountBeanSlots = new CustomObservableMap<>(memberCountBeanSlots);
    }


    /* Getters */

    public CustomObservableMap<Long, MemberCountDisplay> getMemberCountBeanSlots() { return memberCountBeanSlots; }

}
