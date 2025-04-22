package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "beg",
        emoji = "\uD83E\uDD7A",
        executableWithoutArgs = true,
        aliases = { "plz", "please" }
)
public class BegCommand extends RolePlayAbstract {

    public BegCommand(Locale locale, String prefix) {
        super(locale, prefix, false);

        setFtaGifs(
                "https://cdn.discordapp.com/attachments/907017776143683614/907017915247783988/beg.gif",
                "https://cdn.discordapp.com/attachments/907017776143683614/907018117241241610/beg.gif",
                "https://cdn.discordapp.com/attachments/907017776143683614/907018222191132702/beg.gif",
                "https://cdn.discordapp.com/attachments/907017776143683614/907018623535706172/beg.gif",
                "https://cdn.discordapp.com/attachments/907017776143683614/907018723053957170/beg.gif",
                "https://cdn.discordapp.com/attachments/907017776143683614/907018801076391956/beg.gif",
                "https://cdn.discordapp.com/attachments/907017776143683614/907018876284461086/beg.gif",
                "https://cdn.discordapp.com/attachments/907017776143683614/907020211595653180/beg.gif"
        );
        setMtaGifs(
                "https://cdn.discordapp.com/attachments/907017776143683614/907018407935893504/beg.gif",
                "https://cdn.discordapp.com/attachments/907017776143683614/907018499514335342/beg.gif"
        );
    }

}
