package General.Survey;

import org.javacord.api.entity.server.Server;

public class UserMajorityVoteData {
    private Server server;
    private int vote;

    public UserMajorityVoteData(Server server, int vote) {
        this.server = server;
        this.vote = vote;
    }

    public Server getServer() {
        return server;
    }

    public int getVote() {
        return vote;
    }
}
