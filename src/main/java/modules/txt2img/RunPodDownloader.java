package modules.txt2img;

import core.MainLogger;
import core.internet.HttpHeader;
import core.internet.HttpRequest;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class RunPodDownloader {

    private static final Random r = new Random();

    public static CompletableFuture<String> createPrediction(Model model, String prompt, String negativePrompt) {
        JSONObject inputJson = new JSONObject();
        inputJson.put("prompt", prompt);
        if (!negativePrompt.isBlank()) {
            inputJson.put("negative_prompt", negativePrompt);
        }
        inputJson.put("seed", Math.abs(r.nextLong()));
        model.getInputMap().forEach(inputJson::put);

        JSONObject requestJson = new JSONObject();
        requestJson.put("input", inputJson);

        return HttpRequest.post(
                "https://api.runpod.ai/v2/" + model.getModelId() + "/run",
                "application/json",
                requestJson.toString(),
                new HttpHeader("Authorization", "Bearer " + System.getenv("RUNPOD_TOKEN"))
        ).thenApply(response -> {
            if (response.getCode() == 200) {
                return new JSONObject(response.getBody()).getString("id");
            } else {
                throw new CompletionException(new IOException("RunPod error response code " + response.getCode() + ": " + response.getBody()));
            }
        });
    }

    public static CompletableFuture<PredictionResult> retrievePrediction(Model model, String requestId, Instant startTime) {
        return HttpRequest.get(
                "https://api.runpod.ai/v2/" + model.getModelId() + "/status/" + requestId,
                new HttpHeader("Authorization", "Bearer " + System.getenv("RUNPOD_TOKEN"))
        ).thenApply(response -> {
            JSONObject responseJson = new JSONObject(response.getBody());
            PredictionResult.Status status = PredictionResult.Status.valueOf(responseJson.getString("status").toUpperCase());

            PredictionResult.Error error = null;
            if (status == PredictionResult.Status.FAILED) {
                error = extractError(responseJson);
            }

            ArrayList<String> outputs = new ArrayList<>();
            if (status == PredictionResult.Status.COMPLETED) {
                Object output = responseJson.get("output");
                if (output instanceof JSONArray) {
                    JSONArray outputJson = (JSONArray) output;
                    for (int i = 0; i < outputJson.length(); i++) {
                        outputs.add(outputJson.getJSONObject(i).getString("image"));
                    }
                } else if (output instanceof JSONObject) {
                    JSONObject outputJson = (JSONObject) output;
                    outputs.add(outputJson.getString("image_url"));
                } else if (output instanceof String) {
                    outputs.add((String) output);
                }

                if (outputs.isEmpty()) {
                    return PredictionResult.failed(PredictionResult.Error.NSFW);
                }
            }

            double progress = Math.max(0, 1 - Math.pow(0.8, Duration.between(startTime, Instant.now()).toMillis() * model.getTimeMultiplier() / 1000.0));
            return new PredictionResult(
                    Math.min(0.99, progress),
                    status,
                    outputs,
                    error
            );
        });
    }

    private static PredictionResult.Error extractError(JSONObject responseJson) {
        PredictionResult.Error error = null;
        String errorString = null;
        if (responseJson.has("error")) {
            errorString = responseJson.getString("error");
            if (errorString.toLowerCase().contains("nsfw")) {
                error = PredictionResult.Error.NSFW;
            } else {
                error = PredictionResult.Error.GENERAL;
            }
        }
        MainLogger.get().error("RunPod inference failed: {}", errorString);
        return error;
    }

}
