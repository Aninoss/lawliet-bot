package commands.runnables.informationcategory;

import commands.listeners.CommandProperties;

import commands.Command;
import constants.ExternalLinks;
import core.EmbedFactory;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Locale;

@CommandProperties(
        trigger = "upvote",
        emoji = "\uD83D\uDC4D",
        executable = true
)
public class UpvoteCommand extends Command {

    public UpvoteCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("template", ExternalLinks.UPVOTE_URL))).get();
        return true;
    }

}