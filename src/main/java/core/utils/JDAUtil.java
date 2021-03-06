package core.utils;

import constants.Emojis;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

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
    public static RestAction<Message> sendPrivateMessage(Member member, String content) {
        return sendPrivateMessage(member.getUser(), content);
    }

    @CheckReturnValue
    public static RestAction<Message> sendPrivateMessage(User user, String content) {
        return user.openPrivateChannel().flatMap(
                channel -> channel.sendMessage(content)
        );
    }

    @CheckReturnValue
    public static RestAction<Message> sendPrivateMessage(Member member, MessageEmbed eb) {
        return sendPrivateMessage(member.getUser(), eb);
    }

    @CheckReturnValue
    public static RestAction<Message> sendPrivateMessage(User user, MessageEmbed eb) {
        return user.openPrivateChannel().flatMap(
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
