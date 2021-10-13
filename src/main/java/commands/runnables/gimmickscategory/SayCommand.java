package commands.runnables.gimmickscategory;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ExceptionLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

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
    public boolean onTrigger(CommandEvent event, String args) throws ExecutionException, InterruptedException {
        List<Message.Attachment> attachments = event.isGuildMessageReceivedEvent()
                ? event.getGuildMessageReceivedEvent().getMessage().getAttachments()
                : Collections.emptyList();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setDescription(args)
                .setFooter(getString("author", event.getMember().getUser().getAsTag()));

        if (attachments.size() > 0) {
            addLoadingReactionInstantly();
            Message.Attachment attachment = attachments.get(0);
            if (attachment.isImage() && attachment.getSize() <= 8_000_000) {
                String name = "image_main." + attachment.getFileExtension();
                addFileAttachment(attachment.retrieveInputStream().get(), name);
                eb.setImage("attachment://" + name);
            }
        }
        if (attachments.size() > 1) {
            Message.Attachment attachment = attachments.get(1);
            if (attachment.isImage() && attachment.getSize() <= 8_000_000) {
                String name = "image_tn." + attachment.getFileExtension();
                addFileAttachment(attachment.retrieveInputStream().get(), name);
                eb.setThumbnail("attachment://" + name);
            }
        }

        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

}