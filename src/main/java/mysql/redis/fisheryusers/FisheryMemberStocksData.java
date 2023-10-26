package mysql.redis.fisheryusers;

import constants.Settings;
import modules.fishery.Stock;
import modules.fishery.StockMarket;
import mysql.redis.RedisManager;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

public class FisheryMemberStocksData {

    public final String FIELD_SHARES;
    public final String FIELD_INVESTED;

    private final FisheryMemberData fisheryMemberData;
    private final Stock stock;

    public FisheryMemberStocksData(FisheryMemberData fisheryMemberData, Stock stock) {
        this.fisheryMemberData = fisheryMemberData;
        this.stock = stock;
        this.FIELD_SHARES = "stock_num:" + stock.getId();
        this.FIELD_INVESTED = "stock_invested:" + stock.getId();
    }

    public long getInvestedBefore() {
        return RedisManager.getLong(jedis -> jedis.hget(fisheryMemberData.KEY_ACCOUNT, FIELD_INVESTED));
    }

    public long getInvestedAfter() {
        return getShareSize() * StockMarket.getValue(stock);
    }

    public long getShareSize() {
        return RedisManager.getLong(jedis -> jedis.hget(fisheryMemberData.KEY_ACCOUNT, FIELD_SHARES));
    }

    public void add(int number) {
        RedisManager.update(jedis -> {
            Pipeline pipeline = jedis.pipelined();
            Response<String> sizeResp = pipeline.hget(fisheryMemberData.KEY_ACCOUNT, FIELD_SHARES);
            Response<String> investedResp = pipeline.hget(fisheryMemberData.KEY_ACCOUNT, FIELD_INVESTED);
            pipeline.sync();

            long size = RedisManager.parseLong(sizeResp.get());
            long invested = RedisManager.parseLong(investedResp.get());
            long newSize = Math.min(Math.max(size + number, 0), Settings.FISHERY_SHARES_MAX);

            pipeline = jedis.pipelined();
            pipeline.hset(fisheryMemberData.KEY_ACCOUNT, FIELD_SHARES, String.valueOf(newSize));
            if (number >= 0) {
                pipeline.hincrBy(fisheryMemberData.KEY_ACCOUNT, FIELD_INVESTED, Math.round(number * StockMarket.getValue(stock) * (1 + Settings.FISHERY_SHARES_FEES / 100.0)));
            } else {
                if (size == -number) {
                    pipeline.hdel(fisheryMemberData.KEY_ACCOUNT, FIELD_INVESTED);
                } else {
                    long newInvested = Math.round(((double) newSize / size) * invested);
                    pipeline.hset(fisheryMemberData.KEY_ACCOUNT, FIELD_INVESTED, String.valueOf(newInvested));
                }
            }
            pipeline.sync();
        });
    }

}
