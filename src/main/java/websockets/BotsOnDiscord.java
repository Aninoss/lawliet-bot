package websockets;

import core.ExceptionLogger;
import core.ShardManager;
import core.internet.HttpHeader;
import core.internet.HttpRequest;
import org.json.JSONObject;

public class BotsOnDiscord {

    public static void updateServerCount(long serverCount) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("guildCount", serverCount);

        HttpHeader httpHeader = new HttpHeader("Authorization", System.getenv("BOTSONDISCORD_TOKEN"));
        HttpRequest.post("https://bots.ondiscord.xyz/bot-api/bots/" + ShardManager.getSelfId() + "/guilds", "application/json", jsonObject.toString(), httpHeader)
                .exceptionally(ExceptionLogger.get());
    }

}
