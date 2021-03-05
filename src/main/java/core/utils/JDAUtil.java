package core.utils;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
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

}
