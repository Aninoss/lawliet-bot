package core.internet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import core.MainLogger;

public class ParallelInternet {

    private final HashMap<String, CompletableFuture<HttpResponse>> responseHashMap = new HashMap<>();
    final int size;

    public ParallelInternet(String... urlArray) {
        size = urlArray.length;
        Arrays.asList(urlArray).parallelStream().forEach(url -> {
            try {
                if (!responseHashMap.containsKey(url)) {
                    CompletableFuture<HttpResponse> completableFuture = new CompletableFuture<>();
                    responseHashMap.put(url, completableFuture);
                    completableFuture.complete(HttpRequest.getData(url).get());
                }
            } catch (InterruptedException | ExecutionException e) {
                MainLogger.get().error("Could not fetch data", e);
            }
        });
    }

    public HttpResponse get(String url) throws InterruptedException, ExecutionException {
        return responseHashMap.get(url).get();
    }

    public int size() {
        return size;
    }

}
