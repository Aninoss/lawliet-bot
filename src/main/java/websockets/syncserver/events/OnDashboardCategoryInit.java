package websockets.syncserver.events;

import java.util.List;
import java.util.Locale;
import constants.Language;
import core.ShardManager;
import core.TextManager;
import dashboard.DashboardCategory;
import dashboard.DashboardManager;
import net.dv8tion.jda.api.Permission;
import org.json.JSONArray;
import org.json.JSONObject;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

@SyncServerEvent(event = "DASH_CAT_INIT")
public class OnDashboardCategoryInit implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        JSONObject resultJson = new JSONObject();
        long guildId = jsonObject.getLong("guild_id");
        boolean ok = ShardManager.getLocalGuildById(guildId).isPresent();
        resultJson.put("ok", ok);
        if (ok) {
            String categoryId = jsonObject.getString("category");
            long userId = jsonObject.getLong("user_id");
            String localeString = jsonObject.getString("locale");
            Locale locale = Language.from(localeString).getLocale();

            DashboardCategory category = DashboardManager.retrieveCategory(categoryId, guildId, userId, locale);
            List<Permission> missingBotPermissions = category.missingBotPermissions();
            List<Permission> missingUserPermissions = category.missingUserPermissions();
            resultJson.put("missing_bot_permissions", generateMissingPermissionsJson(locale, missingBotPermissions));
            resultJson.put("missing_user_permissions", generateMissingPermissionsJson(locale, missingUserPermissions));
            if (missingBotPermissions.isEmpty() && missingUserPermissions.isEmpty()) {
                resultJson.put("components", category.draw().toJSON());
                DashboardManager.getCategoryCache().put(userId, category);
            }
        }
        return resultJson;
    }

    private JSONArray generateMissingPermissionsJson(Locale locale, List<Permission> permissions) {
        JSONArray permissionsJson = new JSONArray();
        permissions.forEach(p -> {
            String permissionName = TextManager.getString(locale, TextManager.PERMISSIONS, p.getName());
            permissionsJson.put(permissionName);
        });
        return permissionsJson;
    }

}
