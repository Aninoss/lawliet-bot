package core.internet;

import java.util.concurrent.CompletableFuture;
import core.MainLogger;
import core.restclient.RestClient;
import org.json.JSONObject;

public class HttpCache {

    public static CompletableFuture<HttpResponse> get(String url) {
        return RestClient.WEBCACHE.post("webcache", "text/plain", url)
                .thenApply(response -> {
                    JSONObject json = new JSONObject(response.getBody());
                    HttpResponse httpResponse = new HttpResponse()
                            .setCode(json.getInt("code"));
                    if (json.has("body")) {
                        httpResponse.setBody(json.getString("body"));
                    }

                    int code = httpResponse.getCode();
                    if (code / 100 != 2) {
                        MainLogger.get().warn("Error code {} for URL {}", code, url);
                    }

                    return httpResponse;
                });
    }

}