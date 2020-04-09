package MySQL.Modules.AutoRoles;

import Core.CustomObservableList;
import Core.DiscordApiCollection;
import MySQL.Modules.Server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.server.Server;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Optional;

public class AutoRolesBean extends Observable {

    private long serverId;
    private ServerBean serverBean;
    private CustomObservableList<Long> roleIds;

    public AutoRolesBean(long serverId, ServerBean serverBean, @NonNull ArrayList<Long> roleIds) {
        this.serverId = serverId;
        this.serverBean = serverBean;
        this.roleIds = new CustomObservableList<>(roleIds);
    }


    /* Getters */

    public long getServerId() {
        return serverId;
    }

    public Optional<Server> getServer() { return DiscordApiCollection.getInstance().getServerById(serverId); }

    public ServerBean getServerBean() {
        return serverBean;
    }

    public CustomObservableList<Long> getRoleIds() {
        return roleIds;
    }

}
