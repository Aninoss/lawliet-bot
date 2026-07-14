package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import static commands.runnables.informationcategory.HelpCommand.RP_SUBCATEGORY_INTERACTIVE;

@CommandProperties(
        trigger = "bonk",
        emoji = "🏏",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "lewd", "hornyjail", "horny" },
        subCategory = RP_SUBCATEGORY_INTERACTIVE
)
public class BonkCommand extends RolePlayAbstract {

    public BonkCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/826095943748943913/826095979614044180/bonk.gif"
        );
    }

}
