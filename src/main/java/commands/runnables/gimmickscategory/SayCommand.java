package commands.runnables.gimmickscategory;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.utils.CommandUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.BaseGuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
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
        BaseGuildMessageChannel channel;
        CommandUtil.ChannelResponse response = CommandUtil.differentChannelExtract(this, event, args);
        if (response == null) {
            return false;
        } else {
            args = response.getArgs();
            channel = response.getChannel();
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

        CommandUtil.differentChannelSendMessage(this, event, channel, eb, fileAttachmentMap);
        return true;
    }

}