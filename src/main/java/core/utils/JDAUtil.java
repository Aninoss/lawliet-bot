package core.utils;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.CheckReturnValue;
import core.ShardManager;
import core.components.ActionRows;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.Button;
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
        return ShardManager.getAnyJDA().get().openPrivateChannelById(userId).flatMap(
                channel -> channel.sendMessage(content)
        );
    }

    @CheckReturnValue
    public static RestAction<Message> sendPrivateMessage(Member member, MessageEmbed eb, Button... buttons) {
        return sendPrivateMessage(member.getIdLong(), eb, buttons);
    }

    @CheckReturnValue
    public static RestAction<Message> sendPrivateMessage(User user, MessageEmbed eb, Button... buttons) {
        return sendPrivateMessage(user.getIdLong(), eb, buttons);
    }

    @CheckReturnValue
    public static RestAction<Message> sendPrivateMessage(long userId, MessageEmbed eb, Button... buttons) {
        return ShardManager.getAnyJDA().get().openPrivateChannelById(userId).flatMap(
                channel -> channel.sendMessageEmbeds(eb)
                        .setActionRows(ActionRows.of(buttons))
        );
    }

    @CheckReturnValue
    public static RestAction<Message> sendPrivateMessage(long userId, Function<? super PrivateChannel, ? extends RestAction<Message>> flatMap) {
        return ShardManager.getAnyJDA().get().openPrivateChannelById(userId).flatMap(flatMap);
    }

    @CheckReturnValue
    public static RestAction<Message> sendPrivateFile(long userId, InputStream inputStream, String filename) {
        return ShardManager.getAnyJDA().get().openPrivateChannelById(userId).flatMap(
                channel -> channel.sendFile(inputStream, filename)
        );
    }

    @CheckReturnValue
    public static RestAction<Message> sendPrivateFile(long userId, File file) {
        return ShardManager.getAnyJDA().get().openPrivateChannelById(userId).flatMap(
                channel -> channel.sendFile(file)
        );
    }

    public static MessageAction replyMessage(Message originalMessage, String content) {
        MessageAction messageAction = originalMessage.getTextChannel().sendMessage(content);
        messageAction = messageActionSetMessageReference(messageAction, originalMessage);
        return messageAction;
    }

    public static MessageAction replyMessageEmbeds(Message originalMessage, List<MessageEmbed> embeds) {
        MessageAction messageAction = originalMessage.getTextChannel().sendMessageEmbeds(embeds);
        messageAction = messageActionSetMessageReference(messageAction, originalMessage);
        return messageAction;
    }

    public static MessageAction replyMessageEmbeds(Message originalMessage, MessageEmbed embed, MessageEmbed... other) {
        MessageAction messageAction = originalMessage.getTextChannel().sendMessageEmbeds(embed, other);
        messageAction = messageActionSetMessageReference(messageAction, originalMessage);
        return messageAction;
    }

    public static MessageAction messageActionSetMessageReference(MessageAction messageAction, Message originalMessage) {
        return messageActionSetMessageReference(messageAction, originalMessage.getTextChannel(), originalMessage.getIdLong());
    }

    public static MessageAction messageActionSetMessageReference(MessageAction messageAction, TextChannel textChannel, long messageId) {
        if (BotPermissionUtil.can(textChannel, Permission.MESSAGE_HISTORY)) {
            messageAction = messageAction.referenceById(messageId);
        }
        return messageAction;
    }

}
