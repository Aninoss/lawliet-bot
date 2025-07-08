package modules.invitetracking;

import core.assets.MemberAsset;

public class InviteMetrics implements MemberAsset {

    private final long guildId;
    private final long memberId;
    private int totalInvites = 0;
    private int onServer = 0;
    private int retained = 0;
    private int active = 0;

    public InviteMetrics(long guildId, long memberId) {
        this.guildId = guildId;
        this.memberId = memberId;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    @Override
    public long getMemberId() {
        return memberId;
    }

    public int getTotalInvites() {
        return totalInvites;
    }

    public void incrTotalInvites() {
        this.totalInvites++;
    }

    public int getOnServer() {
        return onServer;
    }

    public void incrOnServer() {
        this.onServer++;
    }

    public int getRetained() {
        return retained;
    }

    public void incrRetained() {
        this.retained++;
    }

    public int getActive() {
        return active;
    }

    public void incrActive() {
        this.active++;
    }

}
