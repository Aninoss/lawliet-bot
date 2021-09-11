package modules.invitetracking;

public class InviteMetrics {

    private final int invites;
    private final int activated;
    private final int active;

    public InviteMetrics(int invites, int activated, int active) {
        this.invites = invites;
        this.activated = activated;
        this.active = active;
    }

    public int getInvites() {
        return invites;
    }

    public int getActivated() {
        return activated;
    }

    public int getActive() {
        return active;
    }

}
