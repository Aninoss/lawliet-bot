package commands.runnables.configurationcategory;

import java.util.Locale;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import modules.Prefix;
import net.dv8tion.jda.api.Permission;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "prefix",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "\uD83D\uDCDB",
        executableWithoutArgs = false
)
public class PrefixCommand extends Command {

    public PrefixCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        if (args.length() > 0) {
            if (args.length() <= 5) {
                Prefix.changePrefix(event.getGuild(), getLocale(), args);
                drawMessageNew(EmbedFactory.getEmbedDefault(this, getString("changed", args)))
                        .exceptionally(ExceptionLogger.get());
                return true;
            } else {
                drawMessageNew(EmbedFactory.getEmbedError(
                        this,
                        TextManager.getString(getLocale(), TextManager.GENERAL, "args_too_long", "5")
                )).exceptionally(ExceptionLogger.get());
                return false;
            }
        } else {
            drawMessageNew(EmbedFactory.getEmbedError(
                    this,
                    getString("no_arg")
            )).exceptionally(ExceptionLogger.get());
            return false;
        }
    }

}
