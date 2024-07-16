package commands.runnables.gimmickscategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.utils.CommandUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "say",
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
        GuildMessageChannel channel;
        CommandUtil.ChannelResponse response = CommandUtil.differentChannelExtract(this, event, event.getMessageChannel(), args, Permission.MESSAGE_ATTACH_FILES);
        if (response != null) {
            args = response.getArgs();
            channel = response.getChannel();
        } else {
            return false;
        }

        List<Message.Attachment> attachments = event.isMessageReceivedEvent()
                ? event.getMessageReceivedEvent().getMessage().getAttachments()
                : Collections.emptyList();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setDescription(StringUtil.shortenString(args, 4096))
                .setFooter(getString("author", event.getMember().getUser().getName()));

        Map<String, InputStream> fileAttachmentMap = new HashMap<>();
        if (!attachments.isEmpty()) {
            event.deferReply();
            Message.Attachment attachment = attachments.get(0);
            if (attachment.isImage() && attachment.getSize() <= 8_000_000) {
                String name = "image_main." + attachment.getFileExtension();
                fileAttachmentMap.put(name, attachment.getProxy().download().get());
                eb.setImage("attachment://" + name);
            }
        }
        if (attachments.size() > 1) {
            Message.Attachment attachment = attachments.get(1);
            if (attachment.isImage() && attachment.getSize() <= 8_000_000) {
                String name = "image_tn." + attachment.getFileExtension();
                fileAttachmentMap.put(name, attachment.getProxy().download().get());
                eb.setThumbnail("attachment://" + name);
            }
        }

        CommandUtil.differentChannelSendMessage(this, event, channel, eb, fileAttachmentMap)
                .exceptionally(ExceptionLogger.get());
        return true;
    }

}