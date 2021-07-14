package core.internet;

import java.util.concurrent.CompletableFuture;
import core.GlobalThreadPool;
import core.MainLogger;
import core.restclient.RestClient;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class HttpCache {

    private static final RestClient restClient = RestClient.WEBCACHE;

    public static CompletableFuture<HttpResponse> getData(String url) {
        CompletableFuture<HttpResponse> future = new CompletableFuture<>();
        GlobalThreadPool.getExecutorService().submit(() -> {
            try {
                Invocation.Builder invocationBuilder = restClient.request("webcache", MediaType.APPLICATION_JSON);
                try(Response response = invocationBuilder.post(Entity.text(url))) {
                    HttpResponse httpResponse = response.readEntity(HttpResponse.class);
                    future.complete(httpResponse);

                    int code = httpResponse.getCode();
                    if (code / 100 != 2) {
                        MainLogger.get().warn("Error code {} for URL {}", code, url);
                    }
                }
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

}