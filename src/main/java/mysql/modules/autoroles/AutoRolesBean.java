package mysql.modules.autoroles;

import java.util.ArrayList;
import core.CustomObservableList;
import mysql.BeanWithGuild;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AutoRolesBean extends BeanWithGuild {

    private final CustomObservableList<Long> roleIds;

    public AutoRolesBean(long serverId, @NonNull ArrayList<Long> roleIds) {
        super(serverId);
        this.roleIds = new CustomObservableList<>(roleIds);
    }


    /* Getters */

    public CustomObservableList<Long> getRoleIds() {
        return roleIds;
    }

}
