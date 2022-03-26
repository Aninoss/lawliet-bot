package core.botlists;

import core.ExceptionLogger;
import core.ShardManager;
import core.internet.HttpHeader;
import core.internet.HttpRequest;
import org.json.JSONObject;

public class Discordbotsgg {

    public static void updateServerCount(long serverCount) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("guildCount", serverCount);

        HttpHeader httpHeader = new HttpHeader("Authorization", System.getenv("DISCORDBOTSGG_TOKEN"));
        HttpRequest.post(String.format("https://discord.bots.gg/api/v1/bots/%d/stats", ShardManager.getSelfId()), "application/json", jsonObject.toString(), httpHeader)
                .exceptionally(ExceptionLogger.get());
    }

}
