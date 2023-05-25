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
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
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
            content = content.replace(MentionUtil.getUserAsMention(member.getIdLong(), true), "@" + StringUtil.escapeMarkdownInField(member.getEffectiveName()))
                    .replace(MentionUtil.getUserAsMention(member.getIdLong(), false), "@" + StringUtil.escapeMarkdownInField(member.getEffectiveName()));
        }
        for (TextChannel channel : guild.getTextChannels()) {
            content = content.replace(channel.getAsMention(), "#" + StringUtil.escapeMarkdownInField(channel.getName()));
        }
        for (VoiceChannel channel : guild.getVoiceChannels()) {
            content = content.replace(channel.getAsMention(), "#" + StringUtil.escapeMarkdownInField(channel.getName()));
        }
        for (Role role : guild.getRoles()) {
            content = content.replace(role.getAsMention(), "@" + StringUtil.escapeMarkdownInField(role.getName()));
        }
        for (RichCustomEmoji richCustomEmoji : guild.getEmojis()) {
            content = content.replace(richCustomEmoji.getFormatted(), ":" + StringUtil.escapeMarkdownInField(richCustomEmoji.getName()) + ":");
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

    public static MessageCreateAction replyMessage(Message originalMessage, String content) {
        MessageCreateAction messageAction = originalMessage.getChannel().sendMessage(content);
        messageAction = messageActionSetMessageReference(messageAction, originalMessage);
        return messageAction;
    }

    public static MessageCreateAction replyMessageEmbeds(Message originalMessage, Collection<MessageEmbed> embeds) {
        MessageCreateAction messageAction = originalMessage.getChannel().sendMessageEmbeds(embeds);
        messageAction = messageActionSetMessageReference(messageAction, originalMessage);
        return messageAction;
    }

    public static MessageCreateAction replyMessageEmbeds(Message originalMessage, MessageEmbed embed, MessageEmbed... other) {
        MessageCreateAction messageAction = originalMessage.getChannel().sendMessageEmbeds(embed, other);
        messageAction = messageActionSetMessageReference(messageAction, originalMessage);
        return messageAction;
    }

    public static MessageCreateAction messageActionSetMessageReference(MessageCreateAction messageAction, Message originalMessage) {
        return messageActionSetMessageReference(messageAction, originalMessage.getGuildChannel(), originalMessage.getIdLong());
    }

    public static MessageCreateAction messageActionSetMessageReference(MessageCreateAction messageAction, GuildChannel textChannel, long messageId) {
        if (BotPermissionUtil.can(textChannel, Permission.MESSAGE_HISTORY) &&
                !DBGuild.getInstance().retrieve(textChannel.getGuild().getIdLong()).isCommandAuthorMessageRemoveEffectively()
        ) {
            messageAction = messageAction.setMessageReference(messageId);
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

    public static boolean guildMessageChannelIsNsfw(GuildMessageChannel channel) {
        if (channel instanceof StandardGuildMessageChannel) {
            return ((StandardGuildMessageChannel) channel).isNSFW();
        }
        if (channel instanceof ThreadChannel &&
                ((ThreadChannel) channel).getParentChannel() instanceof GuildMessageChannelUnion
        ) {
            GuildMessageChannelUnion parentMessageChannel = ((ThreadChannel) channel).getParentMessageChannel();
            if (parentMessageChannel instanceof StandardGuildMessageChannel) {
                return ((StandardGuildMessageChannel) parentMessageChannel).isNSFW();
            }
        }
        return false;
    }

}
