package modules.txt2img;

import core.MainLogger;
import core.internet.HttpRequest;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class NsfwDetection {

    private static final List<String> NSFW_LABEL_IDS = List.of("label_5eoodpyns6dosnm5", "label_zbb9xjg778ei3a3x");

    public static boolean isNsfw(String url) {
        JSONObject body = new JSONObject();
        body.put("data", url);

        JSONObject resultJson;
        try {
            String result = HttpRequest.post("https://www.nyckel.com/v1/functions/mcpf3t3w6o3ww7id/invoke?labelCount=5", "application/json", body.toString()).get().getBody();
            resultJson = new JSONObject(result);
        } catch (Throwable e) {
            MainLogger.get().error("NSFW detection error", e);
            return false;
        }

        if (!resultJson.has("labelConfidences")) {
            MainLogger.get().error("NSFW detection error: {}", resultJson);
            return false;
        }

        JSONArray labelConfidencesJson = resultJson.getJSONArray("labelConfidences");
        for (int i = 0; i < labelConfidencesJson.length(); i++) {
            JSONObject confidenceJson = labelConfidencesJson.getJSONObject(i);
            String labelId = confidenceJson.getString("labelId");
            double confidence = confidenceJson.getDouble("confidence");
            if (NSFW_LABEL_IDS.contains(labelId) && confidence >= 0.5) {
                MainLogger.get().info("NSFW image {} generated ({}%)", url, confidence * 100.0);
                return true;
            }
        }

        return false;
    }

}
