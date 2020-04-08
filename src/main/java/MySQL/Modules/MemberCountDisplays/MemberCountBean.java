package MySQL.Modules.MemberCountDisplays;

import General.CustomObservableMap;
import General.DiscordApiCollection;
import MySQL.Modules.Server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.server.Server;

import java.util.HashMap;
import java.util.Observable;
import java.util.Optional;

public class MemberCountBean extends Observable {

    private long serverId;
    private ServerBean serverBean;
    private CustomObservableMap<Long, MemberCountDisplay> memberCountBeanSlots;

    public MemberCountBean(long serverId, ServerBean serverBean, @NonNull HashMap<Long, MemberCountDisplay> memberCountBeanSlots) {
        this.serverId = serverId;
        this.serverBean = serverBean;
        this.memberCountBeanSlots = new CustomObservableMap<>(memberCountBeanSlots);
    }


    /* Getters */

    public long getServerId() {
        return serverId;
    }

    public Optional<Server> getServer() { return DiscordApiCollection.getInstance().getServerById(serverId); }

    public ServerBean getServerBean() {
        return serverBean;
    }

    public CustomObservableMap<Long, MemberCountDisplay> getMemberCountBeanSlots() { return memberCountBeanSlots; }

}
