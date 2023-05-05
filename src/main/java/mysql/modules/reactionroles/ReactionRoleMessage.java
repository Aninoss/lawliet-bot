package mysql.modules.reactionroles;

import java.util.List;
import core.assets.MessageAsset;
import core.atomicassets.AtomicRole;

public class ReactionRoleMessage implements MessageAsset {

    public enum ComponentType { REACTIONS, BUTTONS, SELECT_MENU }

    private final long guildId;
    private final long channelId;
    private final long messageId;
    private final String title;
    private final String desc;
    private final String image;
    private final boolean roleRemoval;
    private final boolean multipleRoles;
    private final ComponentType newComponents;
    private final boolean showRoleNumbers;
    private final boolean showRoleConnections;
    private final List<ReactionRoleMessageSlot> slots;
    private final List<AtomicRole> roleRequirements;

    public ReactionRoleMessage(long guildId, long channelId, long messageId, String title, String desc, String image,
                               boolean roleRemoval, boolean multipleRoles, ComponentType newComponents,
                               boolean showRoleNumbers, Boolean showRoleConnections, List<ReactionRoleMessageSlot> slots,
                               List<AtomicRole> roleRequirements) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.messageId = messageId;
        this.title = title;
        this.desc = desc;
        this.image = image;
        this.roleRemoval = roleRemoval;
        this.multipleRoles = multipleRoles;
        this.newComponents = newComponents;
        this.showRoleNumbers = showRoleNumbers;
        this.showRoleConnections = showRoleConnections != null
                ? showRoleConnections
                : newComponents == ComponentType.REACTIONS;
        this.slots = slots;
        this.roleRequirements = roleRequirements;
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

    public ComponentType getNewComponents() {
        return newComponents;
    }

    public boolean getShowRoleNumbers() {
        return showRoleNumbers;
    }

    public boolean getShowRoleConnections() {
        return showRoleConnections;
    }

    public List<ReactionRoleMessageSlot> getSlots() {
        return slots;
    }

    public List<AtomicRole> getRoleRequirements() {
        return roleRequirements;
    }

}
