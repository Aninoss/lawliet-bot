package core.cache;

import core.DiscordApiManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websockets.syncserver.SendEvent;

import java.util.HashMap;

public class PatreonCache extends SingleCache<HashMap<Long, Integer>> {

    private final static Logger LOGGER = LoggerFactory.getLogger(PatreonCache.class);
    private static final PatreonCache ourInstance = new PatreonCache();

    public static PatreonCache getInstance() {
        return ourInstance;
    }

    private PatreonCache() {}

    public int getUserTier(long userId) {
        if (userId == DiscordApiManager.getInstance().getOwnerId())
            return 6;

        return getAsync().getOrDefault(userId, 0);
    }

    @Override
    protected HashMap<Long, Integer> fetchValue() {
        return SendEvent.sendRequestPatreon().join();
    }

    @Override
    protected int getRefreshRateMinutes() {
        return 5;
    }

}
