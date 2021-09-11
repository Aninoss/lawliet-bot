package mysql.modules.invitetracking;

import java.time.LocalDate;
import core.assets.MemberAsset;
import mysql.DataWithGuild;

public class InviteTrackingSlot extends DataWithGuild implements MemberAsset {

    private final long memberId;
    private final long inviterUserId;
    private final LocalDate invitedDate;
    private LocalDate lastMessage;

    public InviteTrackingSlot(long guildId, long memberId, long inviterUserId, LocalDate invitedDate, LocalDate lastMessage) {
        super(guildId);
        this.memberId = memberId;
        this.inviterUserId = inviterUserId;
        this.invitedDate = invitedDate;
        this.lastMessage = lastMessage;
    }

    @Override
    public long getMemberId() {
        return memberId;
    }

    public long getInviterUserId() {
        return inviterUserId;
    }

    public LocalDate getInvitedDate() {
        return invitedDate;
    }

    public LocalDate getLastMessage() {
        return lastMessage;
    }

    public boolean isActivated() {
        return !lastMessage.isBefore(invitedDate.plusDays(7));
    }

    public boolean isActive() {
        return !LocalDate.now().isAfter(lastMessage.plusDays(7));
    }

    public void messageSent() {
        if (!LocalDate.now().equals(lastMessage)) {
            lastMessage = LocalDate.now();
            setChanged();
            notifyObservers();
        }
    }

}
