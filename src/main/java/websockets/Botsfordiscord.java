package websockets;

import constants.AssetIds;
import core.internet.HttpProperty;
import core.internet.HttpRequest;
import core.internet.HttpResponse;
import core.SecretManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class Botsfordiscord {

    private final static Logger LOGGER = LoggerFactory.getLogger(Botsfordiscord.class);

    public static void updateServerCount(int serverCount) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("server_count", String.valueOf(serverCount));
            HttpProperty[] properties = new HttpProperty[]{
                    new HttpProperty("Content-Type", "application/json"),
                    new HttpProperty("Authorization", SecretManager.getString("botsfordiscord.token"))
            };
            HttpResponse httpResponse = HttpRequest.getData("https://botsfordiscord.com/api/bot/" + AssetIds.LAWLIET_USER_ID, jsonObject.toString(), properties).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Could not post Botsfordiscord request", e);
        }
    }

}
