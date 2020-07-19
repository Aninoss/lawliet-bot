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

public class BotsOnDiscord {

    private final static Logger LOGGER = LoggerFactory.getLogger(BotsOnDiscord.class);

    public static void updateServerCount(int serverCount) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("guildCount", serverCount);
            HttpProperty[] properties = new HttpProperty[]{
                    new HttpProperty("Content-Type", "application/json"),
                    new HttpProperty("Authorization", SecretManager.getString("bots.ondiscord.token"))
            };
            HttpResponse httpResponse = HttpRequest.getData("https://bots.ondiscord.xyz/bot-api/bots/" + Settings.LAWLIET_ID + "/guilds", jsonObject.toString(), properties).get();
        } catch (IOException | InterruptedException | ExecutionException e) {
            LOGGER.error("Could not send data to BotsOnDiscord", e);
        }
    }

}
