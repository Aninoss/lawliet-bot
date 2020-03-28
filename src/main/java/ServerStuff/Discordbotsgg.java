package ServerStuff;

import Constants.Settings;
import General.Internet.Internet;
import General.Internet.InternetProperty;
import General.Internet.InternetResponse;
import General.SecretManager;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Discordbotsgg {

    public static boolean updateServerCount(int serverCount) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("guildCount", serverCount);
            InternetProperty[] properties = new InternetProperty[]{
                    new InternetProperty("Content-Type", "application/json"),
                    new InternetProperty("Authorization", SecretManager.getString("discordbotsgg.token"))
            };
            InternetResponse internetResponse = Internet.getData(String.format("https://discord.bots.gg/api/v1/bots/%d/stats", Settings.LAWLIET_ID), jsonObject.toString(), properties).get();
            return internetResponse.getCode() == 200;
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

}
