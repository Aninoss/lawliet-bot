package General.Tracker;

import com.sun.istack.internal.NotNull;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TrackerData {
    private Server server;
    private ServerTextChannel channel;
    private Message messageDelete;
    private String command, key, arg;
    private Instant instant;
    private long messageId;
    private boolean saveChanges;

    public TrackerData(Server server, ServerTextChannel channel, long messageId, String command, String key, Instant instant, String arg) {
        if (instant == null) throw new NullPointerException();
        this.server = server;
        this.channel = channel;
        this.messageId = messageId;
        this.messageDelete = null;
        this.command = command;
        this.key = key;
        this.instant = instant;
        this.arg = arg;
        this.saveChanges = true;
    }

    public Server getServer() {
        return server;
    }

    public ServerTextChannel getChannel() {
        return channel;
    }

    public Message getMessageDelete() {
        if (messageDelete == null && messageId != 0) {
            try {
                messageDelete = channel.getMessageById(messageId).get(1, TimeUnit.MINUTES);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                //Ignore
            }
        }

        return messageDelete;
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
        this.messageDelete = messageDelete;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setInstant(@NotNull Instant instant) {
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

}
