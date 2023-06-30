package events.sync.events;

import java.util.List;
import java.util.Locale;
import constants.Language;
import core.Program;
import core.ShardManager;
import core.TextManager;
import core.cache.PatreonCache;
import dashboard.DashboardCategory;
import dashboard.DashboardManager;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import net.dv8tion.jda.api.Permission;
import org.json.JSONArray;
import org.json.JSONObject;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;

@SyncServerEvent(event = "DASH_CAT_INIT")
public class OnDashboardCategoryInit implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        JSONObject resultJson = new JSONObject();
        long guildId = jsonObject.getLong("guild_id");
        boolean ok = ShardManager.getLocalGuildById(guildId).isPresent();
        resultJson.put("ok", ok);
        if (ok) {
            resultJson.put("premium", Program.productionMode() && PatreonCache.getInstance().isUnlocked(guildId));
            String categoryId = jsonObject.getString("category");
            long userId = jsonObject.getLong("user_id");
            String localeString = jsonObject.getString("locale");
            boolean createNew = jsonObject.getBoolean("create_new");
            Locale locale = Language.from(localeString).getLocale();

            try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager()) {
                DashboardCategory category = DashboardManager.getCategoryCache().getIfPresent(userId);
                if (createNew || category == null) {
                    category = DashboardManager.retrieveCategory(categoryId, guildId, userId, locale, entityManager);
                } else {
                    category.setEntityManager(entityManager);
                }

                List<Permission> missingBotPermissions = category.missingBotPermissions();
                List<Permission> missingUserPermissions = category.missingUserPermissions();
                resultJson.put("missing_bot_permissions", generateMissingPermissionsJson(locale, missingBotPermissions));
                resultJson.put("missing_user_permissions", generateMissingPermissionsJson(locale, missingUserPermissions));
                if (missingBotPermissions.isEmpty() && missingUserPermissions.isEmpty()) {
                    resultJson.put("components", category.draw().toJSON());
                    DashboardManager.getCategoryCache().put(userId, category);
                }
            }
        }
        return resultJson;
    }

    private JSONArray generateMissingPermissionsJson(Locale locale, List<Permission> permissions) {
        JSONArray permissionsJson = new JSONArray();
        permissions.forEach(p -> {
            String permissionName = TextManager.getString(locale, TextManager.PERMISSIONS, p.name());
            permissionsJson.put(permissionName);
        });
        return permissionsJson;
    }

}
