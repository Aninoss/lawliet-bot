package mysql.modules.ticket;

import java.util.Observable;
import core.assets.MemberAsset;
import core.assets.TextChannelAsset;

public class TicketChannel extends Observable implements TextChannelAsset, MemberAsset {

    private final long guildId;
    private final long channelId;
    private final long userId;
    private final long messageChannelId;
    private final long messageMessageId;
    private boolean assigned;
    private long starterMessageId;
    private final TicketData.TicketAssignmentMode ticketAssignmentMode;

    public TicketChannel(long guildId, long channelId, long userId, long messageChannelId, long messageMessageId,
                         boolean assigned, long starterMessageId, TicketData.TicketAssignmentMode ticketAssignmentMode
    ) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.userId = userId;
        this.messageChannelId = messageChannelId;
        this.messageMessageId = messageMessageId;
        this.assigned = assigned;
        this.starterMessageId = starterMessageId;
        this.ticketAssignmentMode = ticketAssignmentMode;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    @Override
    public long getTextChannelId() {
        return channelId;
    }

    @Override
    public long getMemberId() {
        return userId;
    }

    public long getAnnouncementChannelId() {
        return messageChannelId;
    }

    public long getAnnouncementMessageId() {
        return messageMessageId;
    }

    public long getStarterMessageId() {
        return starterMessageId;
    }

    public void setStarterMessageId(long starterMessageId) {
        this.starterMessageId = starterMessageId;
    }

    public TicketData.TicketAssignmentMode getTicketAssignmentMode() {
        return ticketAssignmentMode;
    }

    public boolean isAssigned() {
        return assigned;
    }

    public TicketChannel setAssigned(boolean assigned) {
        this.assigned = assigned;
        return this;
    }

    public void setAssigned() {
        if (!this.assigned) {
            this.assigned = true;
            setChanged();
            notifyObservers();
        }
    }

}
