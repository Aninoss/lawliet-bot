package core.utils;

import java.util.Optional;
import constants.Emojis;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;

public class JDAEmojiUtil {

    public static boolean emojiIsUnicode(String emoji) {
        return !emoji.contains(":");
    }

    public static long extractIdFromEmoteMention(String emoji) {
        String[] array = emoji.substring(0, emoji.length() - 1).split(":");
        return Long.parseUnsignedLong(array[array.length - 1]);
    }

    public static String emojiAsReactionTag(String mention) {
        return mention.replace("<a:", "")
                .replace("<:", "")
                .replace(">", "");
    }

    public static String reactionEmoteAsMention(MessageReaction.ReactionEmote reactionEmote) {
        if (reactionEmote.isEmoji()) {
            return reactionEmote.getEmoji();
        } else {
            return reactionEmote.getEmote().getAsMention();
        }
    }

    public static boolean reactionEmoteEqualsEmoji(MessageReaction.ReactionEmote reactionEmote, String emoji) {
        return reactionEmoteAsMention(reactionEmote).equals(emoji);
    }

    public static Optional<MessageReaction> getMessageReactionFromMessage(Message message, String emoji) {
        return message.getReactions().stream()
                .filter(r -> r.getReactionEmote().getAsReactionCode().equals(emoji))
                .findFirst();
    }

    public static String getLoadingEmojiMention(TextChannel channel) {
        if (channel != null && BotPermissionUtil.canRead(channel, Permission.MESSAGE_EXT_EMOJI)) {
            return Emojis.LOADING;
        } else {
            return "⏳";
        }
    }

    public static String getLoadingEmojiTag(TextChannel channel) {
        if (channel != null && BotPermissionUtil.canRead(channel, Permission.MESSAGE_EXT_EMOJI)) {
            return JDAEmojiUtil.emojiAsReactionTag(Emojis.LOADING);
        } else {
            return "⏳";
        }
    }

}
