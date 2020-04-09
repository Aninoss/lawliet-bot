package Modules.Fishing;

import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

public class FishingRecords {

    private Server[] servers;
    private User[] users;
    private long[] values;

    public FishingRecords(Server[] servers, User[] users, long[] values) {
        this.servers = servers;
        this.users = users;
        this.values = values;
    }

    public Server getServer(int n) {
        return servers[n];
    }

    public User getUser(int n) {
        return users[n];
    }

    public long getValue(int n) {
        return values[n];
    }
}
