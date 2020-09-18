package mysql.modules.autoroles;

import core.CustomObservableList;
import mysql.BeanWithServer;
import mysql.modules.server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;

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
