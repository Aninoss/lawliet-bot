package commands.runnables.informationcategory;

import java.util.Locale;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import constants.ExternalLinks;
import core.EmbedFactory;
import core.ExceptionLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "upvote",
        emoji = "\uD83D\uDC4D",
        executableWithoutArgs = true
)
public class UpvoteCommand extends Command {

    public UpvoteCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("template", ExternalLinks.UPVOTE_URL));
        setComponents(Button.of(ButtonStyle.LINK, ExternalLinks.UPVOTE_URL, getString("button")));
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

}