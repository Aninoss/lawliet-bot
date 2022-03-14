package commands.runnables.gimmickscategory;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.mention.MentionList;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.BaseGuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "say",
        botChannelPermissions = Permission.MESSAGE_ATTACH_FILES,
        emoji = "\uD83D\uDCAC",
        executableWithoutArgs = true,
        aliases = { "repeat" }
)
public class SayCommand extends Command {

    public SayCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
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

        if (!BotPermissionUtil.canWriteEmbed(channel)) {
            String error = TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel", channel.getAsMention());
            drawMessageNew(EmbedFactory.getEmbedError(this, error))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        List<Message.Attachment> attachments = event.isMessageReceivedEvent()
                ? event.getMessageReceivedEvent().getMessage().getAttachments()
                : Collections.emptyList();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setDescription(args)
                .setFooter(getString("author", event.getMember().getUser().getAsTag()));

        Map<String, InputStream> fileAttachmentMap = new HashMap<>();
        if (attachments.size() > 0) {
            addLoadingReactionInstantly();
            Message.Attachment attachment = attachments.get(0);
            if (attachment.isImage() && attachment.getSize() <= 8_000_000) {
                String name = "image_main." + attachment.getFileExtension();
                fileAttachmentMap.put(name, attachment.retrieveInputStream().get());
                eb.setImage("attachment://" + name);
            }
        }
        if (attachments.size() > 1) {
            Message.Attachment attachment = attachments.get(1);
            if (attachment.isImage() && attachment.getSize() <= 8_000_000) {
                String name = "image_tn." + attachment.getFileExtension();
                fileAttachmentMap.put(name, attachment.retrieveInputStream().get());
                eb.setThumbnail("attachment://" + name);
            }
        }

        if (channel == event.getChannel()) {
            addAllFileAttachments(fileAttachmentMap);
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        } else {
            MessageAction messageAction = channel.sendMessageEmbeds(eb.build());
            for (String name : fileAttachmentMap.keySet()) {
                messageAction = messageAction.addFile(fileAttachmentMap.get(name), name);
            }
            messageAction.queue();

            EmbedBuilder confirmEmbed = EmbedFactory.getEmbedDefault(this, getString("success", channel.getAsMention()));
            drawMessageNew(confirmEmbed).exceptionally(ExceptionLogger.get());
        }
        return true;
    }

}