package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "bonk",
        emoji = "🏏",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "lewd", "hornyjail", "horny", "jail" }
)
public class BonkCommand extends RolePlayAbstract {

    public BonkCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/826095943748943913/826095979614044180/bonk.gif"
        );
    }

}
