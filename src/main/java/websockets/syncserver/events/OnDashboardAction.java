package websockets.syncserver.events;

import java.util.List;
import core.ShardManager;
import core.TextManager;
import dashboard.ActionResult;
import dashboard.DashboardCategory;
import dashboard.DashboardManager;
import net.dv8tion.jda.api.Permission;
import org.json.JSONObject;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

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
                if (missingBotPermissions.isEmpty() && missingUserPermissions.isEmpty()) {
                    ActionResult actionResult = category.receiveAction(jsonObject.getJSONObject("action"));
                    if (actionResult != null) {
                        resultJson.put("ok", true);
                        resultJson.put("redraw", actionResult.getRedraw());
                        resultJson.put("success_message", actionResult.getSuccessMessage());
                        resultJson.put("error_message", actionResult.getErrorMessage());
                    }
                } else {
                    String errorMessage = category.getString(TextManager.GENERAL, "misingpermissions");
                    resultJson.put("ok", true);
                    resultJson.put("redraw", false);
                    resultJson.put("error_message", errorMessage);
                }
            }
        }
        return resultJson;
    }

}
