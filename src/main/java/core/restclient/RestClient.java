package core.restclient;

import core.internet.HttpHeader;
import core.internet.HttpRequest;
import core.internet.HttpResponse;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

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

    public CompletableFuture<HttpResponse> post(String path, String mediaType, String body, HttpHeader... headers) {
        headers = Arrays.copyOf(headers, headers.length + 1);
        headers[headers.length - 1] = new HttpHeader("Authorization", auth);
        return HttpRequest.post(url + path, mediaType, body, headers);
    }

    public CompletableFuture<HttpResponse> get(String path, HttpHeader... headers) {
        headers = Arrays.copyOf(headers, headers.length + 1);
        headers[headers.length - 1] = new HttpHeader("Authorization", auth);
        return HttpRequest.get(url + path, headers);
    }

}
