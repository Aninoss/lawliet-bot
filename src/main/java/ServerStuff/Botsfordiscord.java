package ServerStuff;

import Constants.Settings;
import Core.Internet.Internet;
import Core.Internet.InternetProperty;
import Core.Internet.InternetResponse;
import Core.SecretManager;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Botsfordiscord {

    public static boolean updateServerCount(int serverCount) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("server_count", String.valueOf(serverCount));
            InternetProperty[] properties = new InternetProperty[]{
                    new InternetProperty("Content-Type", "application/json"),
                    new InternetProperty("Authorization", SecretManager.getString("botsfordiscord.token"))
            };
            InternetResponse internetResponse = Internet.getData("https://botsfordiscord.com/api/bot/" + Settings.LAWLIET_ID, jsonObject.toString(), properties).get();
            return internetResponse.getCode() == 200;
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

}
