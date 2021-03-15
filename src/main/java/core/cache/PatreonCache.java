package core.cache;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import constants.AssetIds;
import core.Program;
import org.json.JSONArray;
import org.json.JSONObject;
import websockets.syncserver.SendEvent;

public class PatreonCache extends SingleCache<HashMap<Long, Integer>> {

    private static final PatreonCache ourInstance = new PatreonCache();

    public static PatreonCache getInstance() {
        return ourInstance;
    }

    private PatreonCache() {
    }

    public int getUserTier(long userId) {
        if (userId == AssetIds.OWNER_USER_ID) {
            return 6;
        }

        if (!Program.isProductionMode()) {
            return 0;
        }

        return getAsync().getOrDefault(userId, 0);
    }

    public void requestUpdate() {
        SendEvent.sendEmpty("PATREON_FETCH");
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

    public static HashMap<Long, Integer> userPatreonMapFromJson(JSONObject responseJson) {
        HashMap<Long, Integer> patreonUserTiers = new HashMap<>();
        JSONArray usersArray = responseJson.getJSONArray("users");
        for (int i = 0; i < usersArray.length(); i++) {
            JSONObject userJson = usersArray.getJSONObject(i);
            patreonUserTiers.put(
                    userJson.getLong("user_id"),
                    userJson.getInt("tier")
            );
        }
        return patreonUserTiers;
    }

}
