package core.cache;

import core.Bot;
import core.DiscordApiManager;
import websockets.syncserver.SendEvent;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class PatreonCache extends SingleCache<HashMap<Long, Integer>> {

    private static final PatreonCache ourInstance = new PatreonCache();

    public static PatreonCache getInstance() {
        return ourInstance;
    }

    private PatreonCache() {}

    public int getUserTier(long userId) {
        if (userId == DiscordApiManager.getInstance().getOwnerId())
            return 6;

        if (!Bot.isProductionMode())
            return 0;

        return getAsync().getOrDefault(userId, 0);
    }

    @Override
    protected HashMap<Long, Integer> fetchValue() {
        try {
            return SendEvent.sendRequestPatreon().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected int getRefreshRateMinutes() {
        return 5;
    }

}
