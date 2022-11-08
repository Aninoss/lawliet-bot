package core.utils;

import java.util.Arrays;
import java.util.Optional;
import constants.Emojis;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;

public class EmojiUtil {

    public static boolean equals(Emoji emoji, Emoji otherEmoji) {
        if (emoji instanceof CustomEmoji && otherEmoji instanceof CustomEmoji) {
            return ((CustomEmoji) emoji).getIdLong() == ((CustomEmoji) otherEmoji).getIdLong();
        } else {
            return emoji.getFormatted().equals(otherEmoji.getFormatted());
        }
    }

    public static UnicodeEmoji[] getMultipleFromUnicode(String[] unicode) {
        return Arrays.stream(unicode)
                .map(Emoji::fromUnicode)
                .toArray(UnicodeEmoji[]::new);
    }

    public static Optional<MessageReaction> getMessageReactionFromMessage(Message message, Emoji emoji) {
        return message.getReactions().stream()
                .filter(r -> EmojiUtil.equals(r.getEmoji(), emoji))
                .findFirst();
    }

    public static String getLoadingEmojiMention(TextChannel channel) {
        if (channel != null && BotPermissionUtil.canReadHistory(channel, Permission.MESSAGE_EXT_EMOJI)) {
            return Emojis.LOADING.getFormatted();
        } else {
            return "⏳";
        }
    }

}
