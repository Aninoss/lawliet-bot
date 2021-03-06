package mysql.modules.tracker;

import core.MainLogger;
import mysql.BeanWithServer;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import java.time.Instant;
import java.util.Optional;

public class TrackerBeanSlot extends BeanWithServer {

    private final long channelId;
    private Long messageId;
    private final String commandTrigger, commandKey;
    private String args;
    private Instant nextRequest;
    private boolean active = true;

    public TrackerBeanSlot(long serverId, long channelId, String commandTrigger, Long messageId, String commandKey, Instant nextRequest, String args) {
        super(serverId);
        this.channelId = channelId;
        this.messageId = messageId;
        this.commandTrigger = commandTrigger;
        this.commandKey = commandKey != null ? commandKey : "";
        this.args = args;
        this.nextRequest = nextRequest;
    }


    /* Getters */

    public long getChannelId() { return channelId; }

    public Optional<TextChannel> getChannel() {
        return getGuild().map(guild -> guild.getTextChannelById(channelId));
    }

    public Optional<Long> getMessageId() {
        return Optional.ofNullable(messageId);
    }

    public Optional<Message> getMessage() {
        return getChannel().flatMap(channel -> {
            try {
                return Optional.ofNullable(channel.retrieveMessageById(messageId != null ? messageId : 0L).complete());
            } catch (Throwable e) {
                //Ignore
            }
            return Optional.empty();
        });
    }

    public String getCommandTrigger() { return commandTrigger; }

    public String getCommandKey() { return commandKey; }

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
        stop();

        try {
            DBTracker.getInstance().getBean().getSlots().remove(this);
        } catch (RuntimeException e) {
            MainLogger.get().error("Could not remove tracker", e);
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
