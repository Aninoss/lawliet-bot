package mysql.modules.autoroles;

import java.util.ArrayList;
import core.CustomObservableList;
import mysql.DataWithGuild;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AutoRolesData extends DataWithGuild {

    private final CustomObservableList<Long> roleIds;

    public AutoRolesData(long serverId, @NonNull ArrayList<Long> roleIds) {
        super(serverId);
        this.roleIds = new CustomObservableList<>(roleIds);
    }

    public CustomObservableList<Long> getRoleIds() {
        return roleIds;
    }

}
