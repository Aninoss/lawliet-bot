package websockets;

import constants.Settings;
import core.internet.HttpProperty;
import core.internet.HttpRequest;
import core.SecretManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class Discordbotlist {

    private final static Logger LOGGER = LoggerFactory.getLogger(Discordbotlist.class);

    public static void updateServerCount(int serverCount) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("guilds", serverCount);

            HttpProperty[] properties = new HttpProperty[]{
                    new HttpProperty("Content-Type", "application/json"),
                    new HttpProperty("Authorization", SecretManager.getString("discordbotlist.token"))
            };
            HttpRequest.getData(String.format("https://discordbotlist.com/api/v1/bots/%s/stats", Settings.LAWLIET_ID), jsonObject.toString(), properties).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Could not send data to Discordbotlist", e);
        }
    }

}
