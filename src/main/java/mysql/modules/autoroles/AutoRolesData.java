package mysql.modules.autoroles;

import java.util.ArrayList;
import core.CustomObservableList;
import core.assets.GuildAsset;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AutoRolesData implements GuildAsset {

    private final long guildId;
    private final CustomObservableList<Long> roleIds;

    public AutoRolesData(long serverId, @NonNull ArrayList<Long> roleIds) {
        this.guildId = serverId;
        this.roleIds = new CustomObservableList<>(roleIds);
    }

    public CustomObservableList<Long> getRoleIds() {
        return roleIds;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

}
