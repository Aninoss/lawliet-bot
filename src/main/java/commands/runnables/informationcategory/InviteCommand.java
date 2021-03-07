package commands.runnables.informationcategory;

import commands.listeners.CommandProperties;

import commands.Command;
import constants.ExternalLinks;
import core.EmbedFactory;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Locale;

@CommandProperties(
        trigger = "invite",
        emoji = "\uD83D\uDD17",
        executableWithoutArgs = true,
        onlyPublicVersion = true,
        aliases = {"link", "addbot", "inv"}
)
public class InviteCommand extends Command {

    public InviteCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("template", ExternalLinks.BOT_INVITE_URL))).get();
        return true;
    }
    
}
