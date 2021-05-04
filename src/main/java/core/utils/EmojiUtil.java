package core.utils;

import java.util.Optional;
import java.util.regex.Matcher;
import constants.Emojis;
import constants.RegexPatterns;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;

public class EmojiUtil {

    public static boolean emojiIsUnicode(String emoji) {
        return !emoji.contains(":");
    }

    public static long extractIdFromEmoteMention(String emoji) {
        Matcher m = RegexPatterns.EMOTE.matcher(emoji);
        if (m.find()) {
            return Long.parseLong(m.group("id"));
        }
        return 0L;
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
        long emojiId = extractIdFromEmoteMention(emoji);
        if (emojiId != 0 && reactionEmote.isEmote()) {
            return reactionEmote.getIdLong() == emojiId;
        } else if (emojiId == 0 && reactionEmote.isEmoji()) {
            return reactionEmote.getEmoji().equals(emoji);
        }
        return false;
    }

    public static Optional<MessageReaction> getMessageReactionFromMessage(Message message, String emoji) {
        return message.getReactions().stream()
                .filter(r -> r.getReactionEmote().getAsReactionCode().equals(emoji))
                .findFirst();
    }

    public static String getLoadingEmojiMention(TextChannel channel) {
        if (channel != null && BotPermissionUtil.canReadHistory(channel, Permission.MESSAGE_EXT_EMOJI)) {
            return Emojis.LOADING;
        } else {
            return "⏳";
        }
    }

    public static String getLoadingEmojiTag(TextChannel channel) {
        if (channel != null && BotPermissionUtil.canReadHistory(channel, Permission.MESSAGE_EXT_EMOJI)) {
            return EmojiUtil.emojiAsReactionTag(Emojis.LOADING);
        } else {
            return "⏳";
        }
    }

}
