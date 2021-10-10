package websockets.syncserver.events;

import constants.AssetIds;
import core.ShardManager;
import core.components.ActionRows;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.json.JSONObject;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

@SyncServerEvent(event = "REPORT")
public class OnReport implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        String url = jsonObject.getString("url");
        ShardManager.getLocalGuildById(AssetIds.SUPPORT_SERVER_ID).get()
                .getTextChannelById(896872855248183316L)
                .sendMessage(url)
                .setActionRows(ActionRows.of(Button.of(ButtonStyle.PRIMARY, "allow", "Allow")))
                .complete();
        return null;
    }

}
