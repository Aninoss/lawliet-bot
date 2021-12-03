package websockets.syncserver.events;

import java.util.Locale;
import constants.Language;
import core.ShardManager;
import dashboard.DashboardCategory;
import dashboard.DashboardManager;
import org.json.JSONArray;
import org.json.JSONObject;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

@SyncServerEvent(event = "DASH_INIT")
public class OnDashboardInit implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        JSONObject resultJson = new JSONObject();
        long guildId = jsonObject.getLong("guild_id");
        boolean ok = ShardManager.getLocalGuildById(guildId).isPresent();
        resultJson.put("ok", ok);
        if (ok) {
            long userId = jsonObject.getLong("user_id");
            String localeString = jsonObject.getString("locale");
            Locale locale = Language.from(localeString).getLocale();
            addTitles(guildId, userId, locale, resultJson);
        }
        return resultJson;
    }

    private void addTitles(long guildId, long userId, Locale locale, JSONObject resultJson) {
        JSONArray titlesJson = new JSONArray();
        for (DashboardCategory retrieveCategory : DashboardManager.retrieveCategories(guildId, userId, locale)) {
            JSONObject data = new JSONObject();
            data.put("id", retrieveCategory.getProperties().id());
            data.put("title", retrieveCategory.retrievePageTitle());
            titlesJson.put(data);
        }
        resultJson.put("titles", titlesJson);
    }

}
