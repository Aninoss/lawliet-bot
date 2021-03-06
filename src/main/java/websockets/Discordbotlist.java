package websockets;

import constants.AssetIds;
import core.ExceptionLogger;
import core.internet.HttpProperty;
import core.internet.HttpRequest;
import org.json.JSONObject;

public class Discordbotlist {

    public static void updateServerCount(long serverCount) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("guilds", serverCount);

        HttpProperty[] properties = new HttpProperty[]{
                new HttpProperty("Content-Type", "application/json"),
                new HttpProperty("Authorization", System.getenv("DISCORDBOTLIST_TOKEN"))
        };
        HttpRequest.getData(String.format("https://discordbotlist.com/api/v1/bots/%s/stats", AssetIds.LAWLIET_USER_ID), jsonObject.toString(), properties)
                .exceptionally(ExceptionLogger.get());
    }

}
