package commands.runnables.informationcategory;

import java.util.Locale;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import constants.ExternalLinks;
import core.EmbedFactory;
import core.ExceptionLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "dashboard",
        emoji = "⚙️",
        executableWithoutArgs = true
)
public class DashboardCommand extends Command {

    public DashboardCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        String link = ExternalLinks.DASHBOARD_WEBSITE + "/" + event.getGuild().getId();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("template"));
        setComponents(Button.of(ButtonStyle.LINK, link, getString("button")));
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

}