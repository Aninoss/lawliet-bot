package modules;

import java.util.concurrent.CompletableFuture;
import core.internet.HttpHeader;
import core.internet.HttpRequest;
import core.internet.HttpResponse;

public class DeepAI {

    public static CompletableFuture<HttpResponse> request(String apiUrl, String imageUrl) {
        String query = "image=" + imageUrl;
        HttpHeader header = new HttpHeader("Api-Key", System.getenv("DEEPAI_TOKEN"));
        return HttpRequest.post(apiUrl, "application/x-www-form-urlencoded", query, header);
    }

}
