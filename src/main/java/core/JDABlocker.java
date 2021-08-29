package core;

public class JDABlocker {

    private int totalShards = -1;
    private boolean[] blockedShards;

    public synchronized void add(int totalShards, int shardsMin, int shardsMax) {
        if (!ShardManager.isReady()) {
            return;
        }

        if (totalShards != this.totalShards) {
            this.totalShards = totalShards;
            blockedShards = new boolean[totalShards];
        }

        for (int i = shardsMin; i <= shardsMax; i++) {
            blockedShards[i] = true;
        }
    }

    public boolean guildIsAvailable(long guildId) {
        if (totalShards <= 0 || guildId <= 0L || totalShards == ShardManager.getTotalShards()) {
            return true;
        }

        int shard = ShardManager.getResponsibleShard(guildId, totalShards);
        return !blockedShards[shard];
    }

}
