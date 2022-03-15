package core.utils;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.CommandEvent;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.mention.MentionList;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.BaseGuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

public class CommandUtil {

    public static ChannelResponse differentChannelExtract(Command command, CommandEvent event, String args, Permission... permissions) {
        String[] words = args.split(" ");
        BaseGuildMessageChannel channel = event.getTextChannel();
        MentionList<BaseGuildMessageChannel> messageChannelsFirst = MentionUtil.getBaseGuildMessageChannels(event.getGuild(), words[0]);
        if (messageChannelsFirst.getList().size() > 0) {
            channel = messageChannelsFirst.getList().get(0);
            args = args.substring(words[0].length()).trim();
        } else {
            MentionList<BaseGuildMessageChannel> messageChannelsLast = MentionUtil.getBaseGuildMessageChannels(event.getGuild(), words[words.length - 1]);
            if (messageChannelsLast.getList().size() > 0) {
                channel = messageChannelsLast.getList().get(0);
                args = args.substring(0, args.length() - words[words.length - 1].length()).trim();
            }
        }

        if (!BotPermissionUtil.canWriteEmbed(channel, permissions)) {
            String error = TextManager.getString(command.getLocale(), TextManager.GENERAL, "permission_channel", channel.getAsMention());
            command.drawMessageNew(EmbedFactory.getEmbedError(command, error))
                    .exceptionally(ExceptionLogger.get());
            return null;
        }

        return new ChannelResponse(args, channel);
    }

    public static Message differentChannelSendMessage(Command command, CommandEvent event, BaseGuildMessageChannel channel, EmbedBuilder eb, Map<String, InputStream> fileAttachmentMap) throws ExecutionException, InterruptedException {
        if (event.getChannel() == channel) {
            command.addAllFileAttachments(fileAttachmentMap);
            return command.drawMessageNew(eb).get();
        } else {
            MessageAction messageAction = channel.sendMessageEmbeds(eb.build());
            for (String name : fileAttachmentMap.keySet()) {
                messageAction = messageAction.addFile(fileAttachmentMap.get(name), name);
            }
            Message message = messageAction.complete();

            EmbedBuilder confirmEmbed = EmbedFactory.getEmbedDefault(command, TextManager.getString(command.getLocale(), TextManager.GENERAL, "message_sent_channel", channel.getAsMention()));
            command.drawMessageNew(confirmEmbed).exceptionally(ExceptionLogger.get());

            return message;
        }
    }


    public static class ChannelResponse {

        private final String args;
        private final BaseGuildMessageChannel channel;

        public ChannelResponse(String args, BaseGuildMessageChannel channel) {
            this.args = args;
            this.channel = channel;
        }

        public String getArgs() {
            return args;
        }

        public BaseGuildMessageChannel getChannel() {
            return channel;
        }

    }

}
