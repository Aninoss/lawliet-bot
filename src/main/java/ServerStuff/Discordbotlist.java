package ServerStuff;

import Constants.Settings;
import Core.Internet.Internet;
import Core.Internet.InternetProperty;
import Core.Internet.InternetResponse;
import Core.SecretManager;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Discordbotlist {

    public static boolean updateServerCount(int serverCount) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("guilds", String.valueOf(serverCount));
            InternetProperty[] properties = new InternetProperty[]{
                    new InternetProperty("Content-Type", "application/json"),
                    new InternetProperty("Authorization", "Bot " + SecretManager.getString("discordbotlist.token"))
            };
            InternetResponse internetResponse = Internet.getData(String.format("https://discordbotlist.com/api/bots/%s/stats", Settings.LAWLIET_ID), jsonObject.toString(), properties).get();
            return internetResponse.getCode() == 204;
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

}
