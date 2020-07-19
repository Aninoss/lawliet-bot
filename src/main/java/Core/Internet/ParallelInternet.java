package Core.Internet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ParallelInternet {

    private final static Logger LOGGER = LoggerFactory.getLogger(ParallelInternet.class);
    private HashMap<String, CompletableFuture<HttpResponse>> responseHashMap = new HashMap<>();
    int size;

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
                LOGGER.error("Could not fetch data", e);
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
