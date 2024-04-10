package core.utils;

import core.MemberCacheController;
import core.ShardManager;
import core.atomicassets.MentionableAtomicAsset;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.modules.userprivatechannels.DBUserPrivateChannels;
import mysql.modules.userprivatechannels.PrivateChannelData;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.attribute.IAgeRestrictedChannel;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.attribute.IMemberContainer;
import net.dv8tion.jda.api.entities.channel.attribute.IPositionableChannel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.unions.IThreadContainerUnion;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JDAUtil {

    public static final List<ChannelType> GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES = List.of(ChannelType.TEXT, ChannelType.VOICE, ChannelType.NEWS, ChannelType.STAGE, ChannelType.GUILD_NEWS_THREAD, ChannelType.GUILD_PUBLIC_THREAD, ChannelType.GUILD_PRIVATE_THREAD);
    public static final List<ChannelType> STANDARD_GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES = List.of(ChannelType.TEXT, ChannelType.NEWS);

    public static Optional<TextChannel> getFirstWritableTextChannelOfGuild(Guild guild) {
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
        for (GuildChannel channel : guild.getChannelCache()) {
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

    public static MessageCreateAction replyMessage(Message originalMessage, GuildEntity guildEntity, String content) {
        MessageCreateAction messageAction = originalMessage.getChannel().sendMessage(content);
        messageAction = messageActionSetMessageReference(messageAction, guildEntity, originalMessage);
        return messageAction;
    }

    public static MessageCreateAction replyMessageEmbeds(Message originalMessage, GuildEntity guildEntity, Collection<MessageEmbed> embeds) {
        MessageCreateAction messageAction = originalMessage.getChannel().sendMessageEmbeds(embeds);
        messageAction = messageActionSetMessageReference(messageAction, guildEntity, originalMessage);
        return messageAction;
    }

    public static MessageCreateAction replyMessageEmbeds(Message originalMessage, GuildEntity guildEntity, MessageEmbed embed, MessageEmbed... other) {
        MessageCreateAction messageAction = originalMessage.getChannel().sendMessageEmbeds(embed, other);
        messageAction = messageActionSetMessageReference(messageAction, guildEntity, originalMessage);
        return messageAction;
    }

    public static MessageCreateAction messageActionSetMessageReference(MessageCreateAction messageAction, GuildEntity guildEntity, Message originalMessage) {
        return messageActionSetMessageReference(messageAction, guildEntity, originalMessage.getGuildChannel(), originalMessage.getIdLong());
    }

    public static MessageCreateAction messageActionSetMessageReference(MessageCreateAction messageAction, GuildEntity guildEntity, GuildChannel channel, long messageId) {
        if (BotPermissionUtil.can(channel, Permission.MESSAGE_HISTORY) &&
                !guildEntity.getRemoveAuthorMessageEffectively()
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

    public static boolean channelIsNsfw(Channel channel) {
        if (channel instanceof IAgeRestrictedChannel) {
            return ((IAgeRestrictedChannel) channel).isNSFW();
        }
        if (channel instanceof ThreadChannel) {
            IThreadContainerUnion parentChannel = ((ThreadChannel) channel).getParentChannel();
            if (parentChannel instanceof IAgeRestrictedChannel) {
                return ((IAgeRestrictedChannel) parentChannel).isNSFW();
            }
        }
        return false;
    }

    public static List<Member> getChannelMembers(Channel channel) {
        if (channel instanceof IMemberContainer) {
            return ((IMemberContainer) channel).getMembers();
        }
        return Collections.emptyList();
    }

    public static int getChannelPositionRaw(Channel channel) {
        if (channel instanceof IPositionableChannel) {
            return ((IPositionableChannel) channel).getPositionRaw();
        }
        if (channel instanceof ThreadChannel) {
            IThreadContainerUnion parentChannel = ((ThreadChannel) channel).getParentChannel();
            if (parentChannel instanceof IPositionableChannel) {
                return ((IPositionableChannel) parentChannel).getPositionRaw();
            }
        }
        return -1;
    }

    public static Category getChannelParentCategory(Channel channel) {
        if (channel instanceof ICategorizableChannel) {
            return ((ICategorizableChannel) channel).getParentCategory();
        }
        if (channel instanceof ThreadChannel) {
            IThreadContainerUnion parentChannel = ((ThreadChannel) channel).getParentChannel();
            if (parentChannel instanceof ICategorizableChannel) {
                return ((ICategorizableChannel) parentChannel).getParentCategory();
            }
        }
        return null;
    }

    public static boolean channelOrParentEqualsId(Channel channel, long channelId) {
        if (channel.getIdLong() == channelId) {
            return true;
        }
        if (channel instanceof ThreadChannel) {
            IThreadContainerUnion parentChannel = ((ThreadChannel) channel).getParentChannel();
            if (parentChannel.getIdLong() == channelId) {
                return true;
            }
            if (parentChannel instanceof ICategorizableChannel) {
                Category parentCategory = ((ICategorizableChannel) parentChannel).getParentCategory();
                if (parentCategory != null && parentCategory.getIdLong() == channelId) {
                    return true;
                }
            }
        }
        if (channel instanceof ICategorizableChannel) {
            Category parentCategory = ((ICategorizableChannel) channel).getParentCategory();
            if (parentCategory != null && parentCategory.getIdLong() == channelId) {
                return true;
            }
        }
        return false;
    }

    public static boolean collectionContainsChannelOrParent(Collection<Long> channelIds, Channel channel) {
        if (channelIds.contains(channel.getIdLong())) {
            return true;
        }
        if (channel instanceof ThreadChannel) {
            IThreadContainerUnion parentChannel = ((ThreadChannel) channel).getParentChannel();
            if (channelIds.contains(parentChannel.getIdLong())) {
                return true;
            }
            if (parentChannel instanceof ICategorizableChannel) {
                Category parentCategory = ((ICategorizableChannel) parentChannel).getParentCategory();
                if (parentCategory != null && channelIds.contains(parentCategory.getIdLong())) {
                    return true;
                }
            }
        }
        if (channel instanceof ICategorizableChannel) {
            Category parentCategory = ((ICategorizableChannel) channel).getParentCategory();
            if (parentCategory != null && channelIds.contains(parentCategory.getIdLong())) {
                return true;
            }
        }
        return false;
    }

    public static List<String> toIdList(Collection<? extends MentionableAtomicAsset<?>> collection) {
        return collection.stream()
                .map(MentionableAtomicAsset::getId)
                .collect(Collectors.toList());
    }

}
