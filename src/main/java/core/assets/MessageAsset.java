package core.assets;

import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.entities.Message;

public interface MessageAsset extends TextChannelAsset {

    long getMessageId();

    default CompletableFuture<Message> retrieveMessage() {
        return getTextChannel().map(channel -> channel.retrieveMessageById(getMessageId()).submit())
                .orElseGet(() -> CompletableFuture.failedFuture(new NoSuchElementException("No text channel")));
    }

}
