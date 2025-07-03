package events.sync.apidata.v1;

public class InviteStats {

    private long inviterUserId;
    private int total;
    private int onServer;
    private int retained;
    private int active;

    public long getInviterUserId() {
        return inviterUserId;
    }

    public void setInviterUserId(long inviterUserId) {
        this.inviterUserId = inviterUserId;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getOnServer() {
        return onServer;
    }

    public void setOnServer(int onServer) {
        this.onServer = onServer;
    }

    public int getRetained() {
        return retained;
    }

    public void setRetained(int retained) {
        this.retained = retained;
    }

    public int getActive() {
        return active;
    }

    public void setActive(int active) {
        this.active = active;
    }

}
