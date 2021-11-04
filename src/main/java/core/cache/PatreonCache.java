package core.cache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import constants.AssetIds;
import core.PatreonData;
import org.json.JSONArray;
import org.json.JSONObject;
import websockets.syncserver.SendEvent;

public class PatreonCache extends SingleCache<PatreonData> {

    private static final PatreonCache ourInstance = new PatreonCache();

    public static PatreonCache getInstance() {
        return ourInstance;
    }

    private PatreonCache() {
    }

    public boolean hasPremium(long userId, boolean requiresOld) {
        if (userId == AssetIds.OWNER_USER_ID) {
            return true;
        }

        PatreonData patreonData = getAsync();
        if (patreonData == null ||
                (!patreonData.getOldUserList().contains(userId) && requiresOld)
        ) {
            return false;
        }

        return patreonData.getUserTierMap().containsKey(userId);
    }

    public boolean isUnlocked(long guildId) {
        PatreonData patreonData = getAsync();
        return patreonData != null && patreonData.getGuildList().contains(guildId);
    }

    public void requestUpdate() {
        SendEvent.sendEmpty("PATREON_FETCH");
    }

    @Override
    protected PatreonData fetchValue() {
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

    public static PatreonData patreonDataFromJson(JSONObject responseJson) {
        HashMap<Long, Integer> userTierMap = new HashMap<>();
        HashSet<Long> unlockedGuilds = new HashSet<>();
        HashSet<Long> oldUsers = new HashSet<>();

        JSONArray usersArray = responseJson.getJSONArray("users");
        for (int i = 0; i < usersArray.length(); i++) {
            JSONObject userJson = usersArray.getJSONObject(i);
            long userId = userJson.getLong("user_id");
            int tier = userJson.getInt("tier");
            if (tier > 0) {
                userTierMap.put(userId, tier);
            }
        }

        JSONArray guildsArray = responseJson.getJSONArray("guilds");
        for (int i = 0; i < guildsArray.length(); i++) {
            unlockedGuilds.add(guildsArray.getLong(i));
        }

        JSONArray oldUsersArray = responseJson.getJSONArray("old_users");
        for (int i = 0; i < oldUsersArray.length(); i++) {
            oldUsers.add(oldUsersArray.getLong(i));
        }

        return new PatreonData(userTierMap, unlockedGuilds, oldUsers);
    }

}
