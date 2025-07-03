package events.sync.apidata.v1;

public class Invite {

    private long inviterUserId;
    private long invitedUserId;
    private boolean fakeInvite;
    private boolean onServer;
    private boolean retained;
    private boolean active;

    public long getInviterUserId() {
        return inviterUserId;
    }

    public void setInviterUserId(long inviterUserId) {
        this.inviterUserId = inviterUserId;
    }

    public long getInvitedUserId() {
        return invitedUserId;
    }

    public void setInvitedUserId(long invitedUserId) {
        this.invitedUserId = invitedUserId;
    }

    public boolean isFakeInvite() {
        return fakeInvite;
    }

    public void setFakeInvite(boolean fakeInvite) {
        this.fakeInvite = fakeInvite;
    }

    public boolean isOnServer() {
        return onServer;
    }

    public void setOnServer(boolean onServer) {
        this.onServer = onServer;
    }

    public boolean isRetained() {
        return retained;
    }

    public void setRetained(boolean retained) {
        this.retained = retained;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

}
