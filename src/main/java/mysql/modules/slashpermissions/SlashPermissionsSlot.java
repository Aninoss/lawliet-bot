package mysql.modules.slashpermissions;

import core.assets.*;

public class SlashPermissionsSlot implements GuildAsset, RoleAsset, MemberAsset, GuildChannelAsset {

    public enum Type { ROLE, USER, CHANNEL }

    private final long guildId;
    private final String command;
    private final long objectId;
    private final Type type;
    private final boolean allowed;

    public SlashPermissionsSlot(long guildId, String command, long objectId, Type type, boolean allowed) {
        this.guildId = guildId;
        this.command = command;
        this.objectId = objectId;
        this.type = type;
        this.allowed = allowed;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    public String getCommand() {
        return command;
    }

    public long getObjectId() {
        return objectId;
    }

    public Type getType() {
        return type;
    }

    public boolean isAllowed() {
        return allowed;
    }

    @Override
    public long getRoleId() {
        if (type == Type.ROLE) {
            return objectId;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public long getMemberId() {
        if (type == Type.USER) {
            return objectId;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public long getGuildChannelId() {
        if (type == Type.CHANNEL) {
            return objectId;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public boolean isDefaultObject() {
        if (type != null) {
            return switch (type) {
                case ROLE, CHANNEL -> objectId <= guildId;
                case USER -> false;
            };
        } else {
            return false;
        }
    }

}
