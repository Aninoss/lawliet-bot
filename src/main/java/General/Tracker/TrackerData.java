package General.Tracker;

import General.DiscordApiCollection;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TrackerData {
    private long serverId, channelId, messageId;
    private String command, key, arg;
    private Instant instant;
    private boolean saveChanges;

    public TrackerData(Server server, ServerTextChannel channel, long messageId, String command, String key, Instant instant, String arg) {
        if (instant == null) throw new NullPointerException();
        this.serverId = server.getId();
        this.channelId = channel.getId();
        this.messageId = messageId;
        this.command = command;
        this.key = key;
        this.instant = instant;
        this.arg = arg;
        this.saveChanges = true;
    }

    public void deletePreviousMessage() {
        Optional<Message> messageOptional = getMessageDelete();
        messageOptional.ifPresent(Message::delete);
    }

    public long getServerId() {
        return serverId;
    }

    public long getChannelId() {
        return channelId;
    }

    public Optional<ServerTextChannel> getChannel() {
        return DiscordApiCollection.getInstance().getServerTextChannelById(serverId, channelId);
    }

    public Optional<Message> getMessageDelete() {
        return DiscordApiCollection.getInstance().getMessageById(serverId, channelId, messageId);
    }

    public String getCommand() {
        return command;
    }

    public String getKey() {
        return key;
    }

    public Instant getInstant() {
        if (instant == null) instant = Instant.now();
        return instant;
    }

    public void setMessageDelete(Message messageDelete) {
        this.messageId = messageDelete.getId();
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setInstant(Instant instant) {
        if (instant == null) throw new NullPointerException();
        this.instant = instant;
    }

    public String getArg() {
        return arg;
    }

    public void setArg(String arg) {
        this.arg = arg;
    }

    public void setSaveChanges(boolean saveChanges) {
        this.saveChanges = saveChanges;
    }

    public boolean isSaveChanges() {
        return saveChanges;
    }

    public long getMessageId() {
        return messageId;
    }

}
