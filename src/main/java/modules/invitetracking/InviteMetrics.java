package modules.invitetracking;

import core.assets.MemberAsset;
import org.jetbrains.annotations.NotNull;

public class InviteMetrics implements MemberAsset, Comparable<InviteMetrics> {

    private final long guildId;
    private final long memberId;
    private final int totalInvites;
    private final int onServer;
    private final int retained;
    private final int active;

    public InviteMetrics(long guildId, long memberId, int totalInvites, int onServer, int retained, int active) {
        this.guildId = guildId;
        this.memberId = memberId;
        this.totalInvites = totalInvites;
        this.onServer = onServer;
        this.retained = retained;
        this.active = active;
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

    public int getOnServer() {
        return onServer;
    }

    public int getRetained() {
        return retained;
    }

    public int getActive() {
        return active;
    }

    @Override
    public int compareTo(@NotNull InviteMetrics o) {
        int comp = Integer.compare(totalInvites, o.totalInvites);
        if (comp == 0) {
            comp = Integer.compare(onServer, o.onServer);
            if (comp == 0) {
                comp = Integer.compare(retained, o.retained);
                if (comp == 0) {
                    comp = Integer.compare(active, o.active);
                }
            }
        }
        return comp;
    }

}
