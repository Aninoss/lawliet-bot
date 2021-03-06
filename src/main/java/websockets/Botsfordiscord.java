package websockets;

import constants.AssetIds;
import core.ExceptionLogger;
import core.internet.HttpProperty;
import core.internet.HttpRequest;
import org.json.JSONObject;

public class Botsfordiscord {

    public static void updateServerCount(long serverCount) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("server_count", String.valueOf(serverCount));
        HttpProperty[] properties = new HttpProperty[]{
                new HttpProperty("Content-Type", "application/json"),
                new HttpProperty("Authorization", System.getenv("BOTSFORDISCORD_TOKEN"))
        };
        HttpRequest.getData("https://botsfordiscord.com/api/bot/" + AssetIds.LAWLIET_USER_ID, jsonObject.toString(), properties)
                .exceptionally(ExceptionLogger.get());
    }

}
