package mysql.redis.fisheryusers;

import constants.Settings;
import core.assets.MemberAsset;
import modules.fishery.FisheryPowerUp;
import mysql.redis.RedisManager;
import redis.clients.jedis.Pipeline;

public class FisheryMemberStatsData implements MemberAsset {

    public final String FIELD_ENABLED = "stats:enabled";
    public final String FIELD_TREASURE_CHESTS_OPENED = "stats:treasure_chests_opened";
    public final String FIELD_TREASURE_CHESTS_SUCCESSFUL = "stats:treasure_chests_successful";
    public final String FIELD_TREASURE_CHESTS_TOTAL_COINS_RECEIVED = "stats:treasure_chests_total_coins_received";
    public final String FIELD_POWER_UPS_RECEIVED = "stats:power_ups_received:";

    private final FisheryMemberData fisheryMemberData;

    public FisheryMemberStatsData(FisheryMemberData fisheryMemberData) {
        this.fisheryMemberData = fisheryMemberData;
    }

    @Override
    public long getGuildId() {
        return fisheryMemberData.getGuildId();
    }

    @Override
    public long getMemberId() {
        return fisheryMemberData.getMemberId();
    }

    public boolean getEnabled() {
        return RedisManager.getBoolean(jedis -> jedis.hget(fisheryMemberData.KEY_ACCOUNT, FIELD_ENABLED));
    }

    public void setEnabled(boolean enabled) {
        RedisManager.update(jedis -> {
            Pipeline pipeline = jedis.pipelined();
            FisheryUserManager.setUserActiveOnGuild(pipeline, fisheryMemberData);
            pipeline.hset(fisheryMemberData.KEY_ACCOUNT, FIELD_ENABLED, String.valueOf(enabled));
            pipeline.sync();
        });
    }

    public int getTreasureChestsOpened() {
        return RedisManager.getInteger(jedis -> jedis.hget(fisheryMemberData.KEY_ACCOUNT, FIELD_TREASURE_CHESTS_OPENED));
    }

    public void incrTreasureChestsOpened() {
        if (!getEnabled()) {
            return;
        }

        RedisManager.update(jedis -> {
            Pipeline pipeline = jedis.pipelined();
            FisheryUserManager.setUserActiveOnGuild(pipeline, fisheryMemberData);
            pipeline.hincrBy(fisheryMemberData.KEY_ACCOUNT, FIELD_TREASURE_CHESTS_OPENED, 1);
            pipeline.sync();
        });
    }

    public int getTreasureChestsSuccessful() {
        return RedisManager.getInteger(jedis -> jedis.hget(fisheryMemberData.KEY_ACCOUNT, FIELD_TREASURE_CHESTS_SUCCESSFUL));
    }

    public long getTreasureChestsTotalCoinsReceived() {
        return RedisManager.getLong(jedis -> jedis.hget(fisheryMemberData.KEY_ACCOUNT, FIELD_TREASURE_CHESTS_TOTAL_COINS_RECEIVED));
    }

    public void incrTreasureChestsTotalCoinsReceived(long add) {
        if (!getEnabled()) {
            return;
        }

        RedisManager.update(jedis -> {
            long newValue = Math.min(getTreasureChestsTotalCoinsReceived() + add, Settings.FISHERY_MAX);
            Pipeline pipeline = jedis.pipelined();
            FisheryUserManager.setUserActiveOnGuild(pipeline, fisheryMemberData);
            pipeline.hincrBy(fisheryMemberData.KEY_ACCOUNT, FIELD_TREASURE_CHESTS_SUCCESSFUL, 1);
            pipeline.hset(fisheryMemberData.KEY_ACCOUNT, FIELD_TREASURE_CHESTS_TOTAL_COINS_RECEIVED, String.valueOf(newValue));
            pipeline.sync();
        });
    }

    public int getPowerUpReceived(FisheryPowerUp powerUp) {
        return RedisManager.getInteger(jedis -> jedis.hget(fisheryMemberData.KEY_ACCOUNT, FIELD_POWER_UPS_RECEIVED + powerUp.name().toLowerCase()));
    }

    public void incrPowerUpReceived(FisheryPowerUp powerUp) {
        if (!getEnabled()) {
            return;
        }

        RedisManager.update(jedis -> {
            Pipeline pipeline = jedis.pipelined();
            FisheryUserManager.setUserActiveOnGuild(pipeline, fisheryMemberData);
            pipeline.hincrBy(fisheryMemberData.KEY_ACCOUNT, FIELD_POWER_UPS_RECEIVED + powerUp.name().toLowerCase(), 1);
            pipeline.sync();
        });
    }

}
