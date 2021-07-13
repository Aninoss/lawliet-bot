package core.internet;

import java.util.concurrent.CompletableFuture;
import core.GlobalThreadPool;
import core.MainLogger;
import core.restclient.RestClient;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;

public class HttpCache {

    private static final RestClient restClient = RestClient.webCache();

    public static CompletableFuture<HttpResponse> getData(String url) {
        CompletableFuture<HttpResponse> future = new CompletableFuture<>();
        GlobalThreadPool.getExecutorService().submit(() -> {
            try {
                HttpResponse httpResponse = restClient.request("webcache", MediaType.APPLICATION_JSON)
                        .post(Entity.text(url))
                        .readEntity(HttpResponse.class);
                future.complete(httpResponse);

                int code = httpResponse.getCode();
                if (code / 100 != 2) {
                    MainLogger.get().warn("Error code {} for URL {}", code, url);
                }
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

}