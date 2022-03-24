package core.restclient;

import java.util.concurrent.CompletableFuture;
import core.internet.HttpHeader;
import core.internet.HttpRequest;
import core.internet.HttpResponse;

public class RestClient {

    private final String url;
    private final String auth;

    public static final RestClient RATELIMITER = new RestClient(
            System.getenv("RATELIMITER_HOST"),
            Integer.parseInt(System.getenv("RATELIMITER_PORT")),
            "api/",
            System.getenv("RATELIMITER_AUTH")
    );

    public static final RestClient WEBCACHE = new RestClient(
            System.getenv("WEBCACHE_HOST"),
            Integer.parseInt(System.getenv("WEBCACHE_PORT")),
            "api/",
            System.getenv("WEBCACHE_AUTH")
    );

    public static final RestClient SYNC = new RestClient(
            System.getenv("SYNC_HOST"),
            Integer.parseInt(System.getenv("SYNC_CLIENT_PORT")),
            "api/",
            System.getenv("SYNC_AUTH")
    );

    private RestClient(String domain, int port, String path, String auth) {
        this.url = String.format("http://%s:%d/%s", domain, port, path);
        this.auth = auth;
    }

    public CompletableFuture<HttpResponse> post(String path, String mediaType, String body) {
        return HttpRequest.post(url + path, mediaType, body, new HttpHeader("Authorization", auth));
    }

    public CompletableFuture<HttpResponse> get(String path) {
        return HttpRequest.get(url + path, new HttpHeader("Authorization", auth));
    }

}
