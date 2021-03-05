package mysql.modules.survey;

import core.DiscordApiManager;
import org.javacord.api.entity.server.Server;

import java.util.Optional;

public class SurveySecondVote {

    private final long serverId, userId;
    private final byte vote;

    public SurveySecondVote(long serverId, long userId, byte vote) {
        this.serverId = serverId;
        this.userId = userId;
        this.vote = vote;
    }

    public long getServerId() {
        return serverId;
    }

    public Optional<Server> getServer() {
        return DiscordApiManager.getInstance().getLocalGuildById(getServerId());
    }

    public long getUserId() {
        return userId;
    }

    public byte getVote() {
        return vote;
    }

}
