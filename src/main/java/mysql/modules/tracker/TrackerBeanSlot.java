package mysql.modules.tracker;

import mysql.BeanWithServer;
import mysql.modules.server.ServerBean;
import javafx.util.Pair;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class TrackerBeanSlot extends BeanWithServer {

    private final static Logger LOGGER = LoggerFactory.getLogger(TrackerBeanSlot.class);

    private final long channelId;
    private Long messageId;
    private final String commandTrigger, commandKey;
    private String args;
    private Instant nextRequest;
    private boolean active = true;

    public TrackerBeanSlot(ServerBean serverBean, long channelId, String commandTrigger, Long messageId, String commandKey, Instant nextRequest, String args) {
        super(serverBean);
        this.channelId = channelId;
        this.messageId = messageId;
        this.commandTrigger = commandTrigger;
        this.commandKey = commandKey;
        this.args = args;
        this.nextRequest = nextRequest;
    }


    /* Getters */

    public long getChannelId() { return channelId; }

    public Optional<ServerTextChannel> getChannel() { return getServer().flatMap(server -> server.getTextChannelById(channelId)); }

    public Optional<Long> getMessageId() { return Optional.ofNullable(messageId); }

    public Optional<Message> getMessage() {
        return getChannel().flatMap(channel -> {
            try {
                return Optional.ofNullable(channel.getMessageById(messageId != null ? messageId : 0L).get());
            } catch (InterruptedException | ExecutionException e) {
                //Ignore
            }
            return Optional.empty();
        });
    }

    public String getCommandTrigger() { return commandTrigger; }

    public Optional<String> getCommandKey() { return Optional.ofNullable(commandKey); }

    public Optional<String> getArgs() { return Optional.ofNullable(args); }

    public Instant getNextRequest() { return nextRequest; }


    /* Setters */

    public void setMessageId(Long messageId) {
        if (this.messageId == null || !this.messageId.equals(messageId)) {
            this.messageId = messageId;
        }
    }

    public void setArgs(String args) {
        if (this.args == null || !this.args.equals(args)) {
            this.args = args;
        }
    }

    public void setNextRequest(Instant nextRequest) {
        if (this.nextRequest == null || !this.nextRequest.equals(nextRequest)) {
            this.nextRequest = nextRequest;
        }
    }


    /* Actions */

    public void delete() {
        try {
            DBTracker.getInstance().getBean().getMap().remove(new Pair<>(channelId, commandTrigger));
        } catch (SQLException e) {
            LOGGER.error("Could not remove tracker", e);
        }
    }

    public void stop() {
        active = false;
    }

    public boolean isActive() { return active; }

    public void save() {
        setChanged();
        notifyObservers();
    }

}
