package websockets;

import core.ExceptionLogger;
import core.ShardManager;
import core.internet.HttpHeader;
import core.internet.HttpRequest;
import org.json.JSONObject;

public class Botsfordiscord {

    public static void updateServerCount(long serverCount) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("server_count", String.valueOf(serverCount));

        HttpHeader httpHeader =  new HttpHeader("Authorization", System.getenv("BOTSFORDISCORD_TOKEN"));
        HttpRequest.getData("https://botsfordiscord.com/api/bot/" + ShardManager.getInstance().getSelfId(), "application/json", jsonObject.toString(), httpHeader)
                .exceptionally(ExceptionLogger.get());
    }

}
