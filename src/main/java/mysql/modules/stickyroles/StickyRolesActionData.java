package mysql.modules.stickyroles;

import core.assets.MemberAsset;
import core.assets.RoleAsset;

public class StickyRolesActionData implements MemberAsset, RoleAsset {

    private final long guildId;
    private final long userId;
    private final long roleId;

    public StickyRolesActionData(long guildId, long userId, long roleId) {
        this.guildId = guildId;
        this.userId = userId;
        this.roleId = roleId;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    @Override
    public long getMemberId() {
        return userId;
    }

    @Override
    public long getRoleId() {
        return roleId;
    }

}
