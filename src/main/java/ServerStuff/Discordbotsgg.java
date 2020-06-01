package ServerStuff;

import Constants.Settings;
import Core.Internet.HttpRequest;
import Core.Internet.HttpProperty;
import Core.Internet.HttpResponse;
import Core.SecretManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Discordbotsgg {

    final static Logger LOGGER = LoggerFactory.getLogger(Discordbotsgg.class);

    public static void updateServerCount(int serverCount) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("guildCount", serverCount);
            HttpProperty[] properties = new HttpProperty[]{
                    new HttpProperty("Content-Type", "application/json"),
                    new HttpProperty("Authorization", SecretManager.getString("discordbotsgg.token"))
            };
            HttpResponse httpResponse = HttpRequest.getData(String.format("https://discord.bots.gg/api/v1/bots/%d/stats", Settings.LAWLIET_ID), jsonObject.toString(), properties).get();
        } catch (IOException | InterruptedException | ExecutionException e) {
            LOGGER.error("Could not post request to Discordbots.gg", e);
        }
    }

}
