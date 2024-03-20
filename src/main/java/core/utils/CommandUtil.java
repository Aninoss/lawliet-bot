package core.utils;

import commands.Command;
import commands.CommandEvent;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.atomicassets.AtomicGuildMessageChannel;
import core.mention.MentionList;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CommandUtil {

    public static ChannelResponse differentChannelExtract(Command command, CommandEvent event, GuildMessageChannel defaultChannel, String args, Permission... permissions) {
        String[] words = args.split(" ");
        GuildMessageChannel channel = defaultChannel;
        MentionList<GuildMessageChannel> messageChannelsFirst = MentionUtil.getGuildMessageChannels(event.getGuild(), words[0]);
        if (!messageChannelsFirst.getList().isEmpty()) {
            channel = messageChannelsFirst.getList().get(0);
            args = args.substring(words[0].length()).trim();
        } else {
            MentionList<GuildMessageChannel> messageChannelsLast = MentionUtil.getGuildMessageChannels(event.getGuild(), words[words.length - 1]);
            if (!messageChannelsLast.getList().isEmpty()) {
                channel = messageChannelsLast.getList().get(0);
                args = args.substring(0, args.length() - words[words.length - 1].length()).trim();
            }
        }

        HashSet<Permission> permissionSet = new HashSet<>(List.of(Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS));
        permissionSet.addAll(Arrays.asList(permissions));
        Permission[] finalPermissions = permissionSet.toArray(Permission[]::new);

        if (channel != null) {
            EmbedBuilder missingPermissionsEmbed = BotPermissionUtil.getUserAndBotPermissionMissingEmbed(
                    command.getLocale(),
                    channel,
                    event.getMember(),
                    new Permission[0],
                    finalPermissions,
                    new Permission[0],
                    finalPermissions
            );
            if (missingPermissionsEmbed != null) {
                command.drawMessageNew(missingPermissionsEmbed)
                        .exceptionally(ExceptionLogger.get());
                return null;
            }
        }

        return new ChannelResponse(args, channel);
    }

    public static CompletableFuture<Message> differentChannelSendMessage(Command command, CommandEvent event, GuildMessageChannel channel, EmbedBuilder eb, Map<String, InputStream> fileAttachmentMap) {
        if (event.getChannel() == channel) {
            command.addAllFileAttachments(fileAttachmentMap);
            return command.drawMessageNew(eb);
        } else {
            MessageCreateAction messageAction = channel.sendMessageEmbeds(eb.build());
            for (String name : fileAttachmentMap.keySet()) {
                messageAction = messageAction.addFiles(FileUpload.fromData(fileAttachmentMap.get(name), name));
            }
            CompletableFuture<Message> messageFuture = messageAction.submit();

            EmbedBuilder confirmEmbed = EmbedFactory.getEmbedDefault(command, TextManager.getString(command.getLocale(), TextManager.GENERAL, "message_sent_channel", new AtomicGuildMessageChannel(channel).getPrefixedNameInField(command.getLocale())));
            command.drawMessageNew(confirmEmbed).exceptionally(ExceptionLogger.get());

            return messageFuture;
        }
    }


    public static class ChannelResponse {

        private final String args;
        private final GuildMessageChannel channel;

        public ChannelResponse(String args, GuildMessageChannel channel) {
            this.args = args;
            this.channel = channel;
        }

        public String getArgs() {
            return args;
        }

        public GuildMessageChannel getChannel() {
            return channel;
        }

    }

}
