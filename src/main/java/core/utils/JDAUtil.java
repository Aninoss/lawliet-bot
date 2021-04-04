package core.utils;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.CheckReturnValue;
import core.ShardManager;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;

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

    public static String resolveMentions(Message message, String content) {
        for (Member member : message.getMentionedMembers()) {
            content = content.replace(MentionUtil.getUserAsMention(member.getIdLong(), true), "@" + member.getEffectiveName())
                    .replace(MentionUtil.getUserAsMention(member.getIdLong(), false), "@" + member.getEffectiveName());
        }
        for (TextChannel channel : message.getMentionedChannels()) {
            content = content.replace(channel.getAsMention(), "#" + channel.getName());
        }
        for (Role role : message.getMentionedRoles()) {
            content = content.replace(role.getAsMention(), "@" + role.getName());
        }
        for (Emote emote : message.getEmotes()) {
            content = content.replace(emote.getAsMention(), ":" + emote.getName() + ":");
        }
        return content;
    }

    @CheckReturnValue
    public static RestAction<Message> sendPrivateMessage(Member member, String content) {
        return sendPrivateMessage(member.getIdLong(), content);
    }

    @CheckReturnValue
    public static RestAction<Message> sendPrivateMessage(User user, String content) {
        return sendPrivateMessage(user.getIdLong(), content);
    }

    @CheckReturnValue
    public static RestAction<Message> sendPrivateMessage(long userId, String content) {
        return ShardManager.getInstance().getAnyJDA().get().openPrivateChannelById(userId).flatMap(
                channel -> channel.sendMessage(content)
        );
    }

    @CheckReturnValue
    public static RestAction<Message> sendPrivateMessage(Member member, MessageEmbed eb) {
        return sendPrivateMessage(member.getIdLong(), eb);
    }

    @CheckReturnValue
    public static RestAction<Message> sendPrivateMessage(User user, MessageEmbed eb) {
        return sendPrivateMessage(user.getIdLong(), eb);
    }

    @CheckReturnValue
    public static RestAction<Message> sendPrivateMessage(long userId, MessageEmbed eb) {
        return ShardManager.getInstance().getAnyJDA().get().openPrivateChannelById(userId).flatMap(
                channel -> channel.sendMessage(eb)
        );
    }

    @CheckReturnValue
    public static RestAction<Message> sendPrivateMessage(long userId, Function<? super PrivateChannel, ? extends RestAction<Message>> flatMap) {
        return ShardManager.getInstance().getAnyJDA().get().openPrivateChannelById(userId).flatMap(flatMap);
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
