package core.utils;

import java.util.Collection;
import java.util.Optional;
import javax.annotation.CheckReturnValue;
import core.MemberCacheController;
import core.ShardManager;
import mysql.modules.guild.DBGuild;
import mysql.modules.userprivatechannels.DBUserPrivateChannels;
import mysql.modules.userprivatechannels.PrivateChannelData;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public static String resolveMentions(Guild guild, String content) {
        for (Member member : MemberCacheController.getInstance().loadMembersFull(guild).join()) {
            content = content.replace(MentionUtil.getUserAsMention(member.getIdLong(), true), "@" + member.getEffectiveName())
                    .replace(MentionUtil.getUserAsMention(member.getIdLong(), false), "@" + member.getEffectiveName());
        }
        for (TextChannel channel : guild.getTextChannels()) {
            content = content.replace(channel.getAsMention(), "#" + channel.getName());
        }
        for (VoiceChannel channel : guild.getVoiceChannels()) {
            content = content.replace(channel.getAsMention(), "#" + channel.getName());
        }
        for (Role role : guild.getRoles()) {
            content = content.replace(role.getAsMention(), "@" + role.getName());
        }
        for (Emote emote : guild.getEmotes()) {
            content = content.replace(emote.getAsMention(), ":" + emote.getName() + ":");
        }
        return content;
    }

    @CheckReturnValue
    public static RestAction<PrivateChannel> openPrivateChannel(Member member) {
        return openPrivateChannel(member.getJDA(), member.getIdLong());
    }

    @CheckReturnValue
    public static RestAction<PrivateChannel> openPrivateChannel(User user) {
        return openPrivateChannel(user.getJDA(), user.getIdLong());
    }

    @CheckReturnValue
    public static RestAction<PrivateChannel> openPrivateChannel(JDA jda, long userId) {
        PrivateChannelData privateChannelData = DBUserPrivateChannels.getInstance().retrieve().get(userId);
        if (privateChannelData != null) {
            PrivateChannel privateChannel = generatePrivateChannel(privateChannelData.getPrivateChannelId());
            return new CompletedRestAction<>(jda, privateChannel, null);
        } else {
            return jda.openPrivateChannelById(userId)
                    .map(privateChannel -> {
                        PrivateChannelData newPrivateChannelData = new PrivateChannelData(userId, privateChannel.getIdLong());
                        DBUserPrivateChannels.getInstance().retrieve().put(userId, newPrivateChannelData);
                        return privateChannel;
                    });
        }
    }

    private static PrivateChannel generatePrivateChannel(long privateChannelId) {
        return new PrivateChannel() {

            @Override
            public long getIdLong() {
                return privateChannelId;
            }

            @Override
            public long getLatestMessageIdLong() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean canTalk() {
                return false;
            }

            @Nullable
            @Override
            public User getUser() {
                throw new UnsupportedOperationException();
            }

            @NotNull
            @Override
            public RestAction<User> retrieveUser() {
                throw new UnsupportedOperationException();
            }

            @NotNull
            @Override
            public String getName() {
                throw new UnsupportedOperationException();
            }

            @NotNull
            @Override
            public ChannelType getType() {
                return ChannelType.PRIVATE;
            }

            @NotNull
            @Override
            public JDA getJDA() {
                return ShardManager.getAnyJDA().get();
            }

            @NotNull
            @Override
            public RestAction<Void> delete() {
                throw new UnsupportedOperationException();
            }

        };
    }

    public static MessageAction replyMessage(Message originalMessage, String content) {
        MessageAction messageAction = originalMessage.getChannel().sendMessage(content);
        messageAction = messageActionSetMessageReference(messageAction, originalMessage);
        return messageAction;
    }

    public static MessageAction replyMessageEmbeds(Message originalMessage, Collection<MessageEmbed> embeds) {
        MessageAction messageAction = originalMessage.getChannel().sendMessageEmbeds(embeds);
        messageAction = messageActionSetMessageReference(messageAction, originalMessage);
        return messageAction;
    }

    public static MessageAction replyMessageEmbeds(Message originalMessage, MessageEmbed embed, MessageEmbed... other) {
        MessageAction messageAction = originalMessage.getChannel().sendMessageEmbeds(embed, other);
        messageAction = messageActionSetMessageReference(messageAction, originalMessage);
        return messageAction;
    }

    public static MessageAction messageActionSetMessageReference(MessageAction messageAction, Message originalMessage) {
        return messageActionSetMessageReference(messageAction, originalMessage.getGuildChannel(), originalMessage.getIdLong());
    }

    public static MessageAction messageActionSetMessageReference(MessageAction messageAction, GuildChannel textChannel, long messageId) {
        if (BotPermissionUtil.can(textChannel, Permission.MESSAGE_HISTORY) &&
                !DBGuild.getInstance().retrieve(textChannel.getGuild().getIdLong()).isCommandAuthorMessageRemoveEffectively()
        ) {
            messageAction = messageAction.referenceById(messageId);
        }
        return messageAction;
    }

    public static boolean messageIsUserGenerated(Message message) {
        MessageType messageType = message.getType();
        return messageType == MessageType.DEFAULT ||
                messageType == MessageType.INLINE_REPLY ||
                messageType == MessageType.SLASH_COMMAND ||
                messageType == MessageType.CONTEXT_COMMAND ||
                messageType == MessageType.THREAD_STARTER_MESSAGE;
    }

}
