package mysql.modules.autoroles;

import java.util.List;
import core.CustomObservableList;
import core.assets.GuildAsset;

public class AutoRolesData implements GuildAsset {

    private final long guildId;
    private final CustomObservableList<Long> roleIds;

    public AutoRolesData(long serverId, List<Long> roleIds) {
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
