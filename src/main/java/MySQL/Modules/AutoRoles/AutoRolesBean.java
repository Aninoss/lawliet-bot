package MySQL.Modules.AutoRoles;

import Core.CustomObservableList;
import Core.DiscordApiCollection;
import MySQL.BeanWithServer;
import MySQL.Modules.Server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.server.Server;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Optional;

public class AutoRolesBean extends BeanWithServer {

    private final CustomObservableList<Long> roleIds;

    public AutoRolesBean(ServerBean serverBean, @NonNull ArrayList<Long> roleIds) {
        super(serverBean);
        this.roleIds = new CustomObservableList<>(roleIds);
    }


    /* Getters */

    public CustomObservableList<Long> getRoleIds() {
        return roleIds;
    }

}
