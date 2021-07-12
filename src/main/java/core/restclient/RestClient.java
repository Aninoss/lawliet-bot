package core.restclient;

import java.net.URI;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.UriBuilder;

public class RestClient {

    private final WebTarget target;
    private final String auth;

    private RestClient(String domain, int port, String path, String auth) {
        Client client = ClientBuilder.newClient();
        URI endpoint = UriBuilder
                .fromUri(String.format("http://%s/%s", domain, path))
                .port(port)
                .build();

        target = client.target(endpoint);
        this.auth = auth;
    }

    public Invocation.Builder request(String path, String mediaType) {
        return target.path(path)
                .request(mediaType)
                .header("Authorization", auth);
    }

    public static RestClient ratelimiter() {
        return new RestClient(
                System.getenv("RATELIMITER_HOST"),
                Integer.parseInt(System.getenv("RATELIMITER_PORT")),
                "api/",
                System.getenv("RATELIMITER_AUTH")
        );
    }

    public static RestClient webCache() {
        return new RestClient(
                System.getenv("WEBCACHE_HOST"),
                Integer.parseInt(System.getenv("WEBCACHE_PORT")),
                "api/",
                System.getenv("WEBCACHE_AUTH")
        );
    }

}
