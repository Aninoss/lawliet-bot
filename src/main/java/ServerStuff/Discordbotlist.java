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

public class Discordbotlist {

    final static Logger LOGGER = LoggerFactory.getLogger(Discordbotlist.class);

    public static boolean updateServerCount(int serverCount) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("guilds", String.valueOf(serverCount));
            HttpProperty[] properties = new HttpProperty[]{
                    new HttpProperty("Content-Type", "application/json"),
                    new HttpProperty("Authorization", "Bot " + SecretManager.getString("discordbotlist.token"))
            };
            HttpResponse httpResponse = HttpRequest.getData(String.format("https://discordbotlist.com/api/bots/%s/stats", Settings.LAWLIET_ID), jsonObject.toString(), properties).get();
            return httpResponse.getCode() == 204;
        } catch (IOException | InterruptedException | ExecutionException e) {
            LOGGER.error("Could not send data to Discordbotlist", e);
        }
        return false;
    }

}
