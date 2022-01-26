package core.utils;

import java.util.List;
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
    public static RestAction<MessageChannel> openPrivateChannel(Member member) {
        return openPrivateChannel(member.getJDA(), member.getIdLong());
    }

    @CheckReturnValue
    public static RestAction<MessageChannel> openPrivateChannel(User user) {
        return openPrivateChannel(user.getJDA(), user.getIdLong());
    }

    @CheckReturnValue
    public static RestAction<MessageChannel> openPrivateChannel(JDA jda, long userId) {
        PrivateChannelData privateChannelData = DBUserPrivateChannels.getInstance().retrieve().get(userId);
        if (privateChannelData != null) {
            MessageChannel messageChannel = generatePrivateChannel(userId, privateChannelData.getPrivateChannelId());
            return new CompletedRestAction<>(jda, messageChannel, null);
        } else {
            return jda.openPrivateChannelById(userId)
                    .map(privateChannel -> {
                        PrivateChannelData newPrivateChannelData = new PrivateChannelData(userId, privateChannel.getIdLong());
                        DBUserPrivateChannels.getInstance().retrieve().put(userId, newPrivateChannelData);
                        return privateChannel;
                    });
        }
    }

    private static MessageChannel generatePrivateChannel(long userId, long privateChannelId) {
        return new MessageChannel() {
            @Override
            public long getLatestMessageIdLong() {
                return 0;
            }

            @Override
            public boolean hasLatestMessage() {
                return false;
            }

            @NotNull
            @Override
            public String getName() {
                return String.valueOf(userId);
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

            @Override
            public long getIdLong() {
                return privateChannelId;
            }
        };
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
        if (BotPermissionUtil.can(textChannel, Permission.MESSAGE_HISTORY) &&
                !DBGuild.getInstance().retrieve(textChannel.getGuild().getIdLong()).isCommandAuthorMessageRemoveEffectively()
        ) {
            messageAction = messageAction.referenceById(messageId);
        }
        return messageAction;
    }

}
