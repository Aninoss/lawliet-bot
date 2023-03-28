package mysql.modules.reactionroles;

import java.util.List;
import core.assets.MessageAsset;

public class ReactionRoleMessage implements MessageAsset {

    private final long guildId;
    private final long channelId;
    private final long messageId;
    private final String title;
    private final String desc;
    private final String image;
    private final boolean roleRemoval;
    private final boolean multipleRoles;
    private final boolean newComponents;
    private final List<ReactionRoleMessageSlot> slots;

    public ReactionRoleMessage(long guildId, long channelId, long messageId, String title, String desc, String image,
                               boolean roleRemoval, boolean multipleRoles, boolean newComponents,
                               List<ReactionRoleMessageSlot> slots) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.messageId = messageId;
        this.title = title;
        this.desc = desc;
        this.image = image;
        this.roleRemoval = roleRemoval;
        this.multipleRoles = multipleRoles;
        this.newComponents = newComponents;
        this.slots = slots;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    @Override
    public long getStandardGuildMessageChannelId() {
        return channelId;
    }

    @Override
    public long getMessageId() {
        return messageId;
    }

    public String getTitle() {
        return title;
    }

    public String getDesc() {
        return desc;
    }

    public String getImage() {
        return image;
    }

    public boolean getRoleRemoval() {
        return roleRemoval;
    }

    public boolean getMultipleRoles() {
        return multipleRoles;
    }

    public boolean getNewComponents() {
        return newComponents;
    }

    public List<ReactionRoleMessageSlot> getSlots() {
        return slots;
    }

}
