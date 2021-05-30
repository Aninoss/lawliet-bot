package commands.runnables.informationcategory;

import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import constants.ExternalLinks;
import core.EmbedFactory;
import core.buttons.ButtonStyle;
import core.buttons.MessageButton;
import core.buttons.MessageSendActionAdvanced;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "upvote",
        emoji = "\uD83D\uDC4D",
        onlyPublicVersion = true,
        executableWithoutArgs = true
)
public class UpvoteCommand extends Command {

    public UpvoteCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("template", ExternalLinks.UPVOTE_URL));
        new MessageSendActionAdvanced(event.getChannel())
                .appendButtons(new MessageButton(ButtonStyle.LINK, getString("button"), ExternalLinks.UPVOTE_URL))
                .embed(eb.build())
                .queue();
        return true;
    }

}