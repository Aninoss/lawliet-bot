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
        String content = jsonObject.getString("url");
        if (jsonObject.has("ip_hash")) {
            content += "\nIP Hash: " + jsonObject.getInt("ip_hash");
        }
        if (jsonObject.has("text")) {
            content += "\n```" + jsonObject.getString("text") + "```";
        }

        ShardManager.getLocalGuildById(AssetIds.SUPPORT_SERVER_ID).get()
                .getTextChannelById(896872855248183316L)
                .sendMessage(content)
                .setActionRows(ActionRows.of(
                        Button.of(ButtonStyle.PRIMARY, "allow", "Allow"),
                        Button.of(ButtonStyle.SECONDARY, "lock", "Lock On / Off")
                ))
                .complete();
        return null;
    }

}
