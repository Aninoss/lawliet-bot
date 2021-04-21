package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "merkel",
        emoji = "\uD83C\uDDE9\uD83C\uDDEA",
        executableWithoutArgs = true
)
public class MerkelCommand extends RolePlayAbstract {

    public MerkelCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/736269653948760146/736269660143747082/merkel.png"
        );
    }

}
