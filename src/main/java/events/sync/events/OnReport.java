package events.sync.events;

import constants.AssetIds;
import core.ShardManager;
import core.components.ActionRows;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.json.JSONObject;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;

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
                .getChannelById(GuildMessageChannel.class, 896872855248183316L)
                .sendMessage(content)
                .setComponents(ActionRows.of(
                        Button.of(ButtonStyle.PRIMARY, "allow", "Allow"),
                        Button.of(ButtonStyle.SECONDARY, "lock", "Lock On / Off")
                ))
                .complete();
        return null;
    }

}
