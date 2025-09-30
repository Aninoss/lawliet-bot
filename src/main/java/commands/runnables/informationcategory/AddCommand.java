package commands.runnables.informationcategory;

import java.util.Locale;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import constants.ExternalLinks;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "add",
        emoji = "\uD83D\uDD17",
        executableWithoutArgs = true,
        onlyPublicVersion = true,
        aliases = { "invite", "link", "addbot" }
)
public class AddCommand extends Command {

    public AddCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("template"));
        setComponents(Button.of(ButtonStyle.LINK, ExternalLinks.BOT_INVITE_URL, TextManager.getString(getLocale(), TextManager.GENERAL, "invite_button")));
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

}
