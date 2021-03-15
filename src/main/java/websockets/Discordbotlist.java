package websockets;

import core.ExceptionLogger;
import core.ShardManager;
import core.internet.HttpProperty;
import core.internet.HttpRequest;
import org.json.JSONObject;

public class Discordbotlist {

    public static void updateServerCount(long serverCount) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("guilds", serverCount);

        HttpProperty[] properties = new HttpProperty[] {
                new HttpProperty("Content-Type", "application/json"),
                new HttpProperty("Authorization", System.getenv("DISCORDBOTLIST_TOKEN"))
        };
        HttpRequest.getData(String.format("https://discordbotlist.com/api/v1/bots/%s/stats", ShardManager.getInstance().getSelfId()), jsonObject.toString(), properties)
                .exceptionally(ExceptionLogger.get());
    }

}
