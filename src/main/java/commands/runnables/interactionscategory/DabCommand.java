package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import static commands.runnables.informationcategory.HelpCommand.RP_SUBCATEGORY_NON_INTERACTIVE;

@CommandProperties(
        trigger = "dab",
        emoji = "\uD83C\uDD92",
        executableWithoutArgs = true,
        aliases = { "dap" },
        subCategory = RP_SUBCATEGORY_NON_INTERACTIVE
)
public class DabCommand extends RolePlayAbstract {

    public DabCommand(Locale locale, String prefix) {
        super(locale, prefix, false,
                "https://cdn.discordapp.com/attachments/736253507618865215/736253511095943269/dab.gif",
                "https://cdn.discordapp.com/attachments/736253507618865215/736253518100299796/dab.gif",
                "https://cdn.discordapp.com/attachments/736253507618865215/736253522198396928/dab.gif"
        );
    }

}
