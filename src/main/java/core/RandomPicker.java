package core;

import java.util.concurrent.CompletableFuture;
import core.restclient.RestClient;
import org.json.JSONObject;

public class RandomPicker {

    public static CompletableFuture<Integer> pick(String tag, long guildId, int size) {
        JSONObject json = new JSONObject();
        json.put("tag", tag);
        json.put("guildId", guildId);
        json.put("size", size);

        return RestClient.WEBCACHE.post("random", "application/json", json.toString())
                .thenApply(httpResponse -> Integer.parseInt(httpResponse.getBody()));
    }

}
