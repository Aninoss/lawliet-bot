package websockets.syncserver.events;

import constants.Language;
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
        addTitles(jsonObject, resultJson);
        return resultJson;
    }

    private void addTitles(JSONObject jsonObject, JSONObject resultJson) {
        String localeString = jsonObject.getString("locale");
        for (Language lang : Language.values()) {
            if (lang.getLocale().getDisplayName().toLowerCase().startsWith(localeString)) {
                JSONArray titlesJson = new JSONArray();
                for (DashboardCategory retrieveCategory : DashboardManager.retrieveCategories()) {
                    JSONObject data = new JSONObject();
                    data.put("id", retrieveCategory.getId());
                    data.put("title", retrieveCategory.retrievePageTitle(lang.getLocale()));
                    titlesJson.put(data);
                }
                resultJson.put("titles", titlesJson);
                break;
            }
        }
    }

}
