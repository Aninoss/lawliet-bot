package core;

public class DiscordApiBlocker {

    private int totalShards = -1;
    private boolean[] blockedShards;

    public synchronized void add(int totalShards, int shardsMin, int shardsMax) {
        if (!DiscordApiManager.getInstance().isFullyConnected())
            return;

        if (totalShards != this.totalShards) {
            this.totalShards = totalShards;
            blockedShards = new boolean[totalShards];
        }

        for(int i = shardsMin; i <= shardsMax; i++)
            blockedShards[i] = true;
    }

    public boolean serverIsAvailable(long serverId) {
        if (totalShards <= 0 || serverId <= 0L || totalShards == DiscordApiManager.getInstance().getTotalShards())
            return true;

        int shard = DiscordApiManager.getInstance().getResponsibleShard(serverId, totalShards);
        return !blockedShards[shard];
    }

}
