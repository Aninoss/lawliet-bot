package modules.replicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import core.internet.HttpHeader;
import core.internet.HttpRequest;
import core.internet.HttpResponse;
import modules.DeepAI;
import org.json.JSONArray;
import org.json.JSONObject;

public class ReplicateDownloader {

    public static CompletableFuture<String> createPrediction(Model model, String prompt) {
        JSONObject requestJson = new JSONObject();
        requestJson.put("version", model.getVersion());

        JSONObject inputJson = new JSONObject();
        inputJson.put("prompt", model.getPrefix() + prompt);
        inputJson.put("num_outputs", model.getNumOutputs());
        inputJson.put("negative_prompt", "rating:explicit");
        requestJson.put("input", inputJson);

        return HttpRequest.post(
                "https://api.replicate.com/v1/predictions",
                "application/json",
                requestJson.toString(),
                new HttpHeader("Authorization", "Token " + System.getenv("REPLICATE_TOKEN"))
        ).thenApply(response -> new JSONObject(response.getBody()).getString("id"));
    }

    public static CompletableFuture<PredictionResult> retrievePrediction(String id, boolean checkNsfw) {
        return HttpRequest.get(
                "https://api.replicate.com/v1/predictions/" + id,
                new HttpHeader("Authorization", "Token " + System.getenv("REPLICATE_TOKEN"))
        ).thenApply(response -> {
            JSONObject responseJson = new JSONObject(response.getBody());
            PredictionResult.Status status = PredictionResult.Status.valueOf(responseJson.getString("status").toUpperCase());

            ArrayList<String> outputs = new ArrayList<>();
            ArrayList<CompletableFuture<HttpResponse>> deepAiRequests = new ArrayList<>();
            if (status == PredictionResult.Status.SUCCEEDED) {
                JSONArray outputJson = responseJson.getJSONArray("output");
                for (int i = 0; i < outputJson.length(); i++) {
                    String imageUrl = outputJson.getString(i);
                    if (checkNsfw) {
                        deepAiRequests.add(DeepAI.request("https://api.deepai.org/api/nsfw-detector", imageUrl));
                    }
                    outputs.add(imageUrl);
                }
            }

            double progress = 0;
            if (responseJson.has("logs") && !responseJson.isNull("logs")) {
                String logs = responseJson.getString("logs");
                if (logs.contains("%")) {
                    int p = logs.lastIndexOf("%");
                    String percentString = logs.substring(p - 3, p).trim();
                    progress = Double.parseDouble(percentString) / 100;
                }
            }

            for (CompletableFuture<HttpResponse> deepAiRequest : deepAiRequests) {
                JSONObject deepAiJson = new JSONObject(deepAiRequest.join().getBody());
                double nsfwScore = deepAiJson.getJSONObject("output").getDouble("nsfw_score");
                if (nsfwScore > 0.5) {
                    return new PredictionResult(
                            1.0,
                            PredictionResult.Status.FAILED,
                            Collections.emptyList(),
                            "NSFW"
                    );
                }
            }

            return new PredictionResult(
                    progress,
                    status,
                    outputs,
                    status == PredictionResult.Status.FAILED ? responseJson.getString("error") : null
            );
        });
    }

}
