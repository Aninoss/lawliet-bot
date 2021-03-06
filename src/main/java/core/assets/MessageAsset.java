package core.assets;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface MessageAsset extends TextChannelAsset {

    long getMessageId();

    default CompletableFuture<Message> retrieveMessage() {
        return getTextChannel().map(channel -> channel.retrieveMessageById(getMessageId()).submit())
                .orElseGet(() -> CompletableFuture.failedFuture(new NoSuchElementException("No text channel")));
    }

}
