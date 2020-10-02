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

public class Discordbotsgg {

    private final static Logger LOGGER = LoggerFactory.getLogger(Discordbotsgg.class);

    public static void updateServerCount(int serverCount) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("guildCount", serverCount);
            HttpProperty[] properties = new HttpProperty[]{
                    new HttpProperty("Content-Type", "application/json"),
                    new HttpProperty("Authorization", SecretManager.getString("discordbotsgg.token"))
            };
            HttpResponse httpResponse = HttpRequest.getData(String.format("https://discord.bots.gg/api/v1/bots/%d/stats", AssetIds.LAWLIET_USER_ID), jsonObject.toString(), properties).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Could not post request to Discordbots.gg", e);
        }
    }

}
