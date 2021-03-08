package core.utils;

import java.io.InputStream;
import java.util.Optional;
import javax.annotation.CheckReturnValue;
import constants.Emojis;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

public class JDAUtil {

    public static Optional<TextChannel> getFirstWritableChannelOfGuild(Guild guild) {
        if (guild.getSystemChannel() != null && BotPermissionUtil.canWriteEmbed(guild.getSystemChannel())) {
            return Optional.of(guild.getSystemChannel());
        } else {
            for(TextChannel channel : guild.getTextChannels()) {
                if (BotPermissionUtil.canWriteEmbed(channel)) {
                    return Optional.of(channel);
                }
            }
        }

        return Optional.empty();
    }

    public static String emoteToTag(Emote emote) {
        return (emote.isAnimated() ? "a:" : "") + emote.getName() + ":" + emote.getId();
    }

    @CheckReturnValue
    public static MessageAction sendPrivateMessage(Member member, String content) {
        return sendPrivateMessage(member.getUser(), content);
    }

    @CheckReturnValue
    public static MessageAction sendPrivateMessage(User user, String content) {
        return (MessageAction) user.openPrivateChannel().flatMap(
                channel -> channel.sendMessage(content)
        );
    }

    @CheckReturnValue
    public static MessageAction sendPrivateMessage(Member member, MessageEmbed eb) {
        return sendPrivateMessage(member.getUser(), eb);
    }

    @CheckReturnValue
    public static MessageAction sendPrivateMessage(User user, MessageEmbed eb) {
        return (MessageAction) user.openPrivateChannel().flatMap(
                channel -> channel.sendMessage(eb)
        );
    }

    @CheckReturnValue
    public static RestAction<Message> sendPrivateFile(User user, InputStream inputStream, String filename) {
        return user.openPrivateChannel().flatMap(
                channel -> channel.sendFile(inputStream, filename)
        );
    }

    public static String getLoadingReaction(TextChannel channel) {
        if (BotPermissionUtil.canRead(channel, Permission.MESSAGE_EXT_EMOJI))
            return Emojis.LOADING;
        else return "⏳";
    }

}
