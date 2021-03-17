package core.assets;

import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import core.utils.BotPermissionUtil;
import net.dv8tion.jda.api.entities.Message;

public interface MessageAsset extends TextChannelAsset {

    long getMessageId();

    default CompletableFuture<Message> retrieveMessage() {
        return getTextChannel().map(channel -> {
            if (BotPermissionUtil.canRead(channel)) {
                return channel.retrieveMessageById(getMessageId()).submit();
            } else {
                return null;
            }
        }).orElseGet(() -> CompletableFuture.failedFuture(new NoSuchElementException("No text channel")));
    }

}
