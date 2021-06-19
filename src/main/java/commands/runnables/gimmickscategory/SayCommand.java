package commands.runnables.gimmickscategory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

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
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws ExecutionException, InterruptedException {
        List<Message.Attachment> attachments = event.getMessage().getAttachments();
        HashMap<String, InputStream> attachmentMap = new HashMap<>();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setDescription(args)
                .setFooter(getString("author", event.getMember().getUser().getAsTag()));

        if (attachments.size() > 0) {
            addLoadingReactionInstantly();
            Message.Attachment attachment = attachments.get(0);
            String name = "image_main." + attachment.getFileExtension();
            attachmentMap.put(name, attachment.retrieveInputStream().get());
            eb.setImage("attachment://" + name);
        }
        if (attachments.size() > 1) {
            Message.Attachment attachment = attachments.get(1);
            String name = "image_tn." + attachment.getFileExtension();
            attachmentMap.put(name, attachment.retrieveInputStream().get());
            eb.setThumbnail("attachment://" + name);
        }

        AtomicReference<MessageAction> messageAction = new AtomicReference<>(event.getChannel().sendMessageEmbeds(eb.build()));
        attachmentMap.forEach((name, is) -> messageAction.set(messageAction.get().addFile(is, name)));
        messageAction.get().queue();
        return true;
    }

}