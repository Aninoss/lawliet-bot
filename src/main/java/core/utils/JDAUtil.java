package core.utils;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;
import javax.annotation.CheckReturnValue;
import core.ShardManager;
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

}
