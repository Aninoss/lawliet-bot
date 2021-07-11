package core;

import java.net.URI;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.UriBuilder;

public class RestClient {

    private final WebTarget target;
    private final String auth;

    public RestClient(String domain, int port, String path, String auth) {
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

}
