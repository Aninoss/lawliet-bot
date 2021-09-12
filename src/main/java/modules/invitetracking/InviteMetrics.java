package modules.invitetracking;

public class InviteMetrics {

    private final int totalInvites;
    private final int onServer;
    private final int retained;
    private final int active;

    public InviteMetrics(int totalInvites, int onServer, int retained, int active) {
        this.totalInvites = totalInvites;
        this.onServer = onServer;
        this.retained = retained;
        this.active = active;
    }

    public int getTotalInvites() {
        return totalInvites;
    }

    public int getOnServer() {
        return onServer;
    }

    public int getRetained() {
        return retained;
    }

    public int getActive() {
        return active;
    }

}
