package ServerStuff;

import Constants.Settings;
import General.Internet.Internet;
import General.Internet.InternetResponse;
import General.Pair;
import General.SecretManager;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Discordbotlist {

    public static boolean updateServerCount(int serverCount) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("guilds", String.valueOf(serverCount));
            Pair[] properties = new Pair[]{
                    new Pair<>("Content-Type", "application/json"),
                    new Pair<>("Authorization", "Bot " + SecretManager.getString("discordbotlist.token"))
            };
            InternetResponse internetResponse = Internet.getData(String.format("https://discordbotlist.com/api/bots/%s/stats", Settings.LAWLIET_ID), jsonObject.toString(), properties).get();
            return internetResponse.getCode() == 204;
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

}
