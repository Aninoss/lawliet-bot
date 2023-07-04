package events.sync.events;

import core.ShardManager;
import dashboard.ActionResult;
import dashboard.DashboardCategory;
import dashboard.DashboardManager;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.GuildEntity;
import net.dv8tion.jda.api.Permission;
import org.json.JSONObject;

import java.util.List;

@SyncServerEvent(event = "DASH_ACTION")
public class OnDashboardAction implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        JSONObject resultJson = new JSONObject();
        long guildId = jsonObject.getLong("guild_id");
        resultJson.put("ok", false);
        if (ShardManager.getLocalGuildById(guildId).isPresent()) {
            long userId = jsonObject.getLong("user_id");

            DashboardCategory category = DashboardManager.getCategoryCache().getIfPresent(userId);
            if (category != null) {
                List<Permission> missingBotPermissions = category.missingBotPermissions();
                List<Permission> missingUserPermissions = category.missingUserPermissions();

                try (GuildEntity guildEntity = HibernateManager.findGuildEntity(guildId)) {
                    category.setGuildEntity(guildEntity);

                    if (missingBotPermissions.isEmpty() && missingUserPermissions.isEmpty() && category.anyCommandRequirementsAreAccessible()) {
                        ActionResult actionResult = category.receiveAction(jsonObject.getJSONObject("action"));
                        if (actionResult != null) {
                            resultJson.put("ok", true);
                            resultJson.put("redraw", actionResult.getRedraw());
                            resultJson.put("scroll_to_top", actionResult.getScrollToTop());
                            resultJson.put("success_message", actionResult.getSuccessMessage());
                            resultJson.put("error_message", actionResult.getErrorMessage());
                        }
                    } else {
                        resultJson.put("ok", true);
                        resultJson.put("redraw", true);
                    }
                }
            }
        }
        return resultJson;
    }

}
