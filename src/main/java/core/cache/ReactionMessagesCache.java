package core.cache;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import constants.Emojis;
import core.MainLogger;
import core.emojiconnection.EmojiConnection;
import modules.ReactionMessage;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;

public class ReactionMessagesCache {

    private static final Cache<Long, Optional<ReactionMessage>> reactionMessageCache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofHours(12))
            .build();

    public static void put(long messageId, ReactionMessage reactionMessage) {
        reactionMessageCache.put(messageId, Optional.ofNullable(reactionMessage));
    }

    public static synchronized Optional<ReactionMessage> get(TextChannel channel, long messageId) {
        if (reactionMessageCache.asMap().containsKey(messageId)) {
            return reactionMessageCache.getIfPresent(messageId);
        } else {
            Optional<ReactionMessage> reactionMessageOpt;
            try {
                Message message = MessageCache.retrieveMessage(channel, messageId).get();
                reactionMessageOpt = generateReactionMessage(message);
            } catch (Throwable e) {
                reactionMessageOpt = Optional.empty();
            }

            reactionMessageCache.put(messageId, reactionMessageOpt);
            return reactionMessageOpt;
        }
    }

    public static Optional<ReactionMessage> get(Message message) {
        if (reactionMessageCache.asMap().containsKey(message.getIdLong())) {
            return reactionMessageCache.getIfPresent(message.getIdLong());
        } else {
            Optional<ReactionMessage> reactionMessageOpt;
            try {
                reactionMessageOpt = generateReactionMessage(message);
            } catch (Throwable throwable) {
                MainLogger.get().error("Reaction roles message error", throwable);
                reactionMessageOpt = Optional.empty();
            }
            reactionMessageCache.put(message.getIdLong(), reactionMessageOpt);
            return reactionMessageOpt;
        }
    }

    private static Optional<ReactionMessage> generateReactionMessage(Message message) throws Throwable {
        MessageEmbed embed = message.getEmbeds().get(0);
        String title = embed.getTitle();

        int hiddenNumber = -1;
        while (title.endsWith(Emojis.FULL_SPACE_UNICODE)) {
            title = title.substring(0, title.length() - 1);
            hiddenNumber++;
        }
        title = title.substring(3).trim();
        boolean removeRole = (hiddenNumber & 0x1) <= 0;
        boolean multipleRoles = (hiddenNumber & 0x2) <= 0;

        String description = null;
        if (embed.getDescription() != null) {
            description = embed.getDescription().trim();
        }

        String banner = null;
        if (embed.getImage() != null) {
            banner = embed.getImage().getUrl();
        }

        ArrayList<EmojiConnection> emojiConnections = new ArrayList<>();
        for (String line : embed.getFields().get(0).getValue().split("\n")) {
            String[] parts = line.split(" → ");
            emojiConnections.add(new EmojiConnection(parts[0], parts[1]));
        }

        ReactionMessage reactionMessage = new ReactionMessage(
                message.getGuild().getIdLong(),
                message.getTextChannel().getIdLong(),
                message.getIdLong(),
                title,
                description,
                banner,
                removeRole,
                multipleRoles,
                emojiConnections
        );
        return Optional.of(reactionMessage);
    }

}
