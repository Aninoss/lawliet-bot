package ServerStuff;

import Constants.Settings;
import Core.Internet.Internet;
import Core.Internet.InternetProperty;
import Core.Internet.InternetResponse;
import Core.SecretManager;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class BotsOnDiscord {

    public static boolean updateServerCount(int serverCount) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("guildCount", serverCount);
            InternetProperty[] properties = new InternetProperty[]{
                    new InternetProperty("Content-Type", "application/json"),
                    new InternetProperty("Authorization", SecretManager.getString("bots.ondiscord.token"))
            };
            InternetResponse internetResponse = Internet.getData("https://bots.ondiscord.xyz/bot-api/bots/" + Settings.LAWLIET_ID + "/guilds", jsonObject.toString(), properties).get();
            return internetResponse.getCode() == 204;
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

}
