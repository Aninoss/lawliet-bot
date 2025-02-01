package core.restclient;

import core.ConsistentHash;

import java.util.List;

public class DistributedRestClients {

    private final ConsistentHash<String> domainHash;
    private final String localDomain;
    private final int port;
    private final String path;
    private final String auth;

    public DistributedRestClients(List<String> domains, String localDomain, int port, String path, String auth) {
        this.domainHash = new ConsistentHash<>(domains, 10);
        this.localDomain = localDomain;
        this.port = port;
        this.path = path;
        this.auth = auth;
    }

    public RestClient getClient(String key) {
        return new RestClient(domainHash.get(key.toLowerCase()), port, path, auth);
    }

    public RestClient getLocalClient() {
        return new RestClient(localDomain, port, path, auth);
    }

}
