package mysql.modules.stickyroles;

import java.util.List;
import core.CustomObservableList;
import core.assets.GuildAsset;

public class StickyRolesData implements GuildAsset {

    private final long guildId;
    private final CustomObservableList<Long> roleIds;
    private final CustomObservableList<StickyRolesActionData> actions;

    public StickyRolesData(long serverId, List<Long> roleIds, List<StickyRolesActionData> actions) {
        this.guildId = serverId;
        this.roleIds = new CustomObservableList<>(roleIds);
        this.actions = new CustomObservableList<>(actions);
    }

    public CustomObservableList<Long> getRoleIds() {
        return roleIds;
    }

    public CustomObservableList<StickyRolesActionData> getActions() {
        return actions;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

}
