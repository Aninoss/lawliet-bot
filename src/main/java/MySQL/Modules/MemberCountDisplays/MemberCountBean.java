package MySQL.Modules.MemberCountDisplays;

import Core.CustomObservableMap;
import Core.DiscordApiCollection;
import MySQL.BeanWithServer;
import MySQL.Modules.Server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.server.Server;

import java.util.HashMap;
import java.util.Observable;
import java.util.Optional;

public class MemberCountBean extends BeanWithServer {

    private final CustomObservableMap<Long, MemberCountDisplay> memberCountBeanSlots;

    public MemberCountBean(ServerBean serverBean, @NonNull HashMap<Long, MemberCountDisplay> memberCountBeanSlots) {
        super(serverBean);
        this.memberCountBeanSlots = new CustomObservableMap<>(memberCountBeanSlots);
    }


    /* Getters */

    public CustomObservableMap<Long, MemberCountDisplay> getMemberCountBeanSlots() { return memberCountBeanSlots; }

}
