package commands.runnables.utilitycategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.CommandOnOffSwitchAbstract;
import mysql.modules.autoquote.DBAutoQuote;
import net.dv8tion.jda.api.Permission;

@CommandProperties(
        trigger = "autoquote",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "\uD83D\uDCDD",
        executableWithoutArgs = true
)
public class AutoQuoteCommand extends CommandOnOffSwitchAbstract {

    public AutoQuoteCommand(Locale locale, String prefix) {
        super(locale, prefix, false);
    }

    @Override
    protected boolean isActive() {
        return DBAutoQuote.getInstance().retrieve(getGuildId().get()).isActive();
    }

    @Override
    protected void setActive(boolean active) {
        DBAutoQuote.getInstance().retrieve(getGuildId().get()).setActive(active);
    }

}
