package core.cache;

import java.util.ArrayList;
import java.util.HashMap;
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

    public int getUserTier(long userId, boolean requiresOld) {
        if (userId == AssetIds.OWNER_USER_ID) {
            return 6;
        }

        PatreonData patreonData = getAsync();
        if (patreonData == null ||
                (!patreonData.getOldUsersList().contains(userId) && requiresOld)
        ) {
            return 0;
        }

        return patreonData.getUserMap().getOrDefault(userId, 0);
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
        HashMap<Long, Integer> patreonUserTiers = new HashMap<>();
        ArrayList<Long> unlockedGuilds = new ArrayList<>();
        ArrayList<Long> oldUsers = new ArrayList<>();

        JSONArray usersArray = responseJson.getJSONArray("users");
        for (int i = 0; i < usersArray.length(); i++) {
            JSONObject userJson = usersArray.getJSONObject(i);
            patreonUserTiers.put(
                    userJson.getLong("user_id"),
                    userJson.getInt("tier")
            );
        }

        JSONArray guildsArray = responseJson.getJSONArray("guilds");
        for (int i = 0; i < guildsArray.length(); i++) {
            unlockedGuilds.add(guildsArray.getLong(i));
        }

        JSONArray oldUsersArray = responseJson.getJSONArray("old_users");
        for (int i = 0; i < oldUsersArray.length(); i++) {
            oldUsers.add(oldUsersArray.getLong(i));
        }

        return new PatreonData(patreonUserTiers, unlockedGuilds, oldUsers);
    }

}
