package core.internet;

import java.util.concurrent.CompletableFuture;
import core.MainLogger;
import core.restclient.RestClient;

public class HttpCache {

    public static CompletableFuture<HttpResponse> get(String url) {
        HttpHeader[] headers = new HttpHeader[] {
                new HttpHeader("X-Proxy-Url", url),
                new HttpHeader("X-Proxy-Minutes", "5")
        };
        return RestClient.WEBCACHE.get("cached_proxy", headers)
                .thenApply(httpResponse -> {
                    checkResponseCode(url, httpResponse);
                    return httpResponse;
                });
    }

    public static CompletableFuture<HttpResponse> post(String url, String body, String contentType) {
        HttpHeader[] headers = new HttpHeader[] {
                new HttpHeader("X-Proxy-Url", url),
                new HttpHeader("X-Proxy-Minutes", "5")
        };
        return RestClient.WEBCACHE.post("cached_proxy", contentType, body, headers)
                .thenApply(httpResponse -> {
                    checkResponseCode(url, httpResponse);
                    return httpResponse;
                });
    }

    private static void checkResponseCode(String url, HttpResponse httpResponse) {
        int code = httpResponse.getCode();
        if (code / 100 != 2) {
            MainLogger.get().warn("Error code {} for URL {}", code, url);
        }
    }

}