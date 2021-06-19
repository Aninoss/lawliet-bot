package commands.runnables.informationcategory;

import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import constants.ExternalLinks;
import core.EmbedFactory;
import core.components.ActionRows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

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
        event.getChannel().sendMessageEmbeds(eb.build())
                .setActionRows(ActionRows.of(Button.of(ButtonStyle.LINK, ExternalLinks.UPVOTE_URL, getString("button"))))
                .queue();
        return true;
    }

}