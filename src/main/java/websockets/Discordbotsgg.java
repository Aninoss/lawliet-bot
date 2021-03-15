package websockets;

import core.ExceptionLogger;
import core.ShardManager;
import core.internet.HttpProperty;
import core.internet.HttpRequest;
import org.json.JSONObject;

public class Discordbotsgg {

    public static void updateServerCount(long serverCount) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("guildCount", serverCount);
        HttpProperty[] properties = new HttpProperty[] {
                new HttpProperty("Content-Type", "application/json"),
                new HttpProperty("Authorization", System.getenv("DISCORDBOTSGG_TOKEN"))
        };
        HttpRequest.getData(String.format("https://discord.bots.gg/api/v1/bots/%d/stats", ShardManager.getInstance().getSelfId()), jsonObject.toString(), properties)
                .exceptionally(ExceptionLogger.get());
    }

}
