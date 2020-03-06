package General.Internet;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ParallelInternet {

    private HashMap<String, CompletableFuture<InternetResponse>> responseHashMap = new HashMap<>();
    int size;

    public ParallelInternet(String... urlArray) {
        size = urlArray.length;
        Arrays.asList(urlArray).parallelStream().forEach(url -> {
            try {
                if (!responseHashMap.containsKey(url)) {
                    CompletableFuture<InternetResponse> completableFuture = new CompletableFuture<>();
                    responseHashMap.put(url, completableFuture);
                    completableFuture.complete(Internet.getData(url).get());
                }
            } catch (IOException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }

    public InternetResponse get(String url) throws InterruptedException, ExecutionException {
        return responseHashMap.get(url).get();
    }

    public int size() {
        return size;
    }

}
