package mysql.modules.reactionroles;

import core.assets.RoleAsset;
import net.dv8tion.jda.api.entities.emoji.Emoji;

public class ReactionRoleMessageSlot implements RoleAsset {

    private final long serverId;
    private final Emoji emoji;
    private final long roleId;

    public ReactionRoleMessageSlot(long serverId, Emoji emoji, long roleId) {
        this.serverId = serverId;
        this.emoji = emoji;
        this.roleId = roleId;
    }

    @Override
    public long getGuildId() {
        return serverId;
    }

    public Emoji getEmoji() {
        return emoji;
    }

    @Override
    public long getRoleId() {
        return roleId;
    }

}
