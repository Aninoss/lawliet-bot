package core.utils;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;
import javax.annotation.CheckReturnValue;
import constants.Emojis;
import core.ShardManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

public class JDAUtil {

    public static Optional<TextChannel> getFirstWritableChannelOfGuild(Guild guild) {
        if (guild.getSystemChannel() != null && BotPermissionUtil.canWriteEmbed(guild.getSystemChannel())) {
            return Optional.of(guild.getSystemChannel());
        } else {
            for (TextChannel channel : guild.getTextChannels()) {
                if (BotPermissionUtil.canWriteEmbed(channel)) {
                    return Optional.of(channel);
                }
            }
        }

        return Optional.empty();
    }

    public static String emoteToEmoji(Emote emote) {
        return emote.getName() + ":" + emote.getId();
    }

    public static boolean emojiIsUnicode(String emoji) {
        return !emoji.contains(":");
    }

    public static Optional<String> emojiAsMention(String emoji) {
        if (emojiIsUnicode(emoji)) {
            return Optional.of(emoji);
        } else {
            return emoteFromEmoji(emoji).map(Emote::getAsMention);
        }
    }

    public static long extractIdFromEmote(String emoji) {
        return Long.parseUnsignedLong(emoji.split(":")[1]);
    }

    public static Optional<Emote> emoteFromEmoji(String emoji) {
        long id = extractIdFromEmote(emoji);
        return ShardManager.getInstance().getLocalEmoteById(id);
    }

    public static String emojiFromMention(String mention) {
        return mention.replace("<a:", "")
                .replace("<:", "")
                .replace(">", "");
    }

    public static Optional<MessageReaction> getMessageReactionFromMessage(Message message, String emoji) {
        return message.getReactions().stream()
                .filter(r -> r.getReactionEmote().getAsReactionCode().equals(emoji))
                .findFirst();
    }

    @CheckReturnValue
    public static MessageAction sendPrivateMessage(Member member, String content) {
        return sendPrivateMessage(member.getIdLong(), content);
    }

    @CheckReturnValue
    public static MessageAction sendPrivateMessage(User user, String content) {
        return sendPrivateMessage(user.getIdLong(), content);
    }

    @CheckReturnValue
    public static MessageAction sendPrivateMessage(long userId, String content) {
        return (MessageAction) ShardManager.getInstance().getAnyJDA().get().openPrivateChannelById(userId).flatMap(
                channel -> channel.sendMessage(content)
        );
    }

    @CheckReturnValue
    public static MessageAction sendPrivateMessage(Member member, MessageEmbed eb) {
        return sendPrivateMessage(member.getIdLong(), eb);
    }

    @CheckReturnValue
    public static MessageAction sendPrivateMessage(User user, MessageEmbed eb) {
        return sendPrivateMessage(user.getIdLong(), eb);
    }

    @CheckReturnValue
    public static MessageAction sendPrivateMessage(long userId, MessageEmbed eb) {
        return (MessageAction) ShardManager.getInstance().getAnyJDA().get().openPrivateChannelById(userId).flatMap(
                channel -> channel.sendMessage(eb)
        );
    }

    @CheckReturnValue
    public static RestAction<Message> sendPrivateFile(long userId, InputStream inputStream, String filename) {
        return ShardManager.getInstance().getAnyJDA().get().openPrivateChannelById(userId).flatMap(
                channel -> channel.sendFile(inputStream, filename)
        );
    }

    @CheckReturnValue
    public static RestAction<Message> sendPrivateFile(long userId, File file) {
        return ShardManager.getInstance().getAnyJDA().get().openPrivateChannelById(userId).flatMap(
                channel -> channel.sendFile(file)
        );
    }

    public static String getLoadingReaction(TextChannel channel) {
        if (channel != null && BotPermissionUtil.canRead(channel, Permission.MESSAGE_EXT_EMOJI)) {
            return Emojis.LOADING;
        } else {
            return "⏳";
        }
    }

}
