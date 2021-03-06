package websockets;

import constants.AssetIds;
import core.ExceptionLogger;
import core.internet.HttpProperty;
import core.internet.HttpRequest;
import org.json.JSONObject;

public class BotsOnDiscord {

    public static void updateServerCount(long serverCount) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("guildCount", serverCount);
        HttpProperty[] properties = new HttpProperty[]{
                new HttpProperty("Content-Type", "application/json"),
                new HttpProperty("Authorization", System.getenv("BOTSONDISCORD_TOKEN"))
        };
        HttpRequest.getData("https://bots.ondiscord.xyz/bot-api/bots/" + AssetIds.LAWLIET_USER_ID + "/guilds", jsonObject.toString(), properties)
                .exceptionally(ExceptionLogger.get());
    }

}
