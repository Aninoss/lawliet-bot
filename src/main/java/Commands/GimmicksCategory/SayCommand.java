package Commands.GimmicksCategory;

import CommandListeners.CommandProperties;
import CommandSupporters.Command;
import Core.EmbedFactory;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "say",
        emoji = "\uD83D\uDCAC",
        executable = false,
        aliases = { "repeat" }
)
public class SayCommand extends Command {

    public SayCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        List<MessageAttachment> attachments = event.getMessage().getAttachments();
        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setDescription(followedString)
                .setFooter(event.getMessage().getUserAuthor().get().getDiscriminatedName());

        if (attachments.size() > 0) {
            MessageAttachment attachment = attachments.get(0);
            if (attachment.getUrl().toString().endsWith("gif"))
                eb.setImage(attachment.getUrl().toString());
            else
                eb.setImage(attachment.downloadAsInputStream());
        }
        if (attachments.size() > 1) {
            MessageAttachment attachment = attachments.get(1);
            if (attachment.getUrl().toString().endsWith("gif"))
                eb.setThumbnail(attachment.getUrl().toString());
            else
                eb.setThumbnail(attachment.downloadAsInputStream());
        }

        event.getChannel().sendMessage(eb).get();
        return true;
    }

}