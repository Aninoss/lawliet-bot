package core.botlists;

import core.ExceptionLogger;
import core.ShardManager;
import core.internet.HttpHeader;
import core.internet.HttpRequest;
import org.json.JSONObject;

public class BotsForDiscord {

    public static void updateServerCount(long serverCount) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("server_count", serverCount);

        HttpHeader httpHeader =  new HttpHeader("Authorization", System.getenv("BOTSFORDISCORD_TOKEN"));
        HttpRequest.post("https://discords.com/bots/api/bot/" + ShardManager.getSelfId(), "application/json", jsonObject.toString(), httpHeader)
                .exceptionally(ExceptionLogger.get());
    }

}
