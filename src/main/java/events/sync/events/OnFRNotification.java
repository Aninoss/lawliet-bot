package events.sync.events;

import constants.AssetIds;
import core.EmbedFactory;
import core.ShardManager;
import core.components.ActionRows;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import org.json.JSONObject;

@SyncServerEvent(event = "FR_NOTIFICATION")
public class OnFRNotification implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        String title = jsonObject.getString("title");
        String desc = jsonObject.getString("desc");
        int id = jsonObject.getInt("id");

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(title.isEmpty() ? "-" : title)
                .setDescription(desc)
                .setFooter(String.format("%d", id));

        ShardManager.getLocalGuildById(AssetIds.SUPPORT_SERVER_ID).get()
                .getChannelById(GuildMessageChannel.class, 1031135108033429534L)
                .sendMessageEmbeds(eb.build())
                .setComponents(ActionRows.of(
                        Button.of(ButtonStyle.PRIMARY, "true", "Accept"),
                        Button.of(ButtonStyle.SECONDARY, "false", "Deny")
                ))
                .complete();

        return null;
    }

}