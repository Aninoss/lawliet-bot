package websockets;

import core.ExceptionLogger;
import core.ShardManager;
import core.internet.HttpProperty;
import core.internet.HttpRequest;
import org.json.JSONObject;

public class Botsfordiscord {

    public static void updateServerCount(long serverCount) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("server_count", String.valueOf(serverCount));
        HttpProperty[] properties = new HttpProperty[] {
                new HttpProperty("Content-Type", "application/json"),
                new HttpProperty("Authorization", System.getenv("BOTSFORDISCORD_TOKEN"))
        };
        HttpRequest.getData("https://botsfordiscord.com/api/bot/" + ShardManager.getInstance().getSelfId(), jsonObject.toString(), properties)
                .exceptionally(ExceptionLogger.get());
    }

}
