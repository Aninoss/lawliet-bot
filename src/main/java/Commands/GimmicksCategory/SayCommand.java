package Commands.GimmicksCategory;

import CommandListeners.CommandProperties;
import CommandSupporters.Command;
import Core.EmbedFactory;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.List;

@CommandProperties(
        trigger = "say",
        emoji = "\uD83D\uDCAC",
        executable = false,
        aliases = {"repeat"}
)
public class SayCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        List<MessageAttachment> attachments = event.getMessage().getAttachments();
        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setDescription(followedString)
                .setFooter(event.getMessage().getUserAuthor().get().getDiscriminatedName());
        if (attachments.size() > 0) eb.setImage(attachments.get(0).getUrl().toString());
        if (attachments.size() > 1) eb.setThumbnail(attachments.get(1).getUrl().toString());

        event.getChannel().sendMessage(eb).get();
        return true;
    }

}
