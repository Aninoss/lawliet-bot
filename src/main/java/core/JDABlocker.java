package core;

public class JDABlocker {

    private int totalShards = -1;
    private boolean[] blockedShards;

    public synchronized void add(int totalShards, int shardsMin, int shardsMax) {
        if (!ShardManager.getInstance().isReady())
            return;

        if (totalShards != this.totalShards) {
            this.totalShards = totalShards;
            blockedShards = new boolean[totalShards];
        }

        for(int i = shardsMin; i <= shardsMax; i++)
            blockedShards[i] = true;
    }

    public boolean guildIsAvailable(long guildId) {
        if (totalShards <= 0 || guildId <= 0L || totalShards == ShardManager.getInstance().getTotalShards())
            return true;

        int shard = ShardManager.getInstance().getResponsibleShard(guildId, totalShards);
        return !blockedShards[shard];
    }

}
