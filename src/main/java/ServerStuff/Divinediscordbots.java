package ServerStuff;

import Constants.Settings;
import General.Internet.Internet;
import General.Internet.InternetResponse;
import General.Pair;
import General.SecretManager;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Divinediscordbots {

    public static boolean updateServerCount(int serverCount) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("server_count", serverCount);
            Pair[] properties = new Pair[]{
                    new Pair<>("Content-Type", "application/json"),
                    new Pair<>("Authorization", SecretManager.getString("divinediscordbots.token"))
            };
            InternetResponse internetResponse = Internet.getData(String.format("https://divinediscordbots.com/bot/%d/stats", Settings.LAWLIET_ID), jsonObject.toString(), properties).get();
            return internetResponse.getCode() == 200;
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

}
