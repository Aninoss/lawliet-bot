package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "marry",
        emoji = "\uD83D\uDC8D",
        executableWithoutArgs = true,
        requiresFullMemberCache = true
)
public class MarryCommand extends RolePlayAbstract {

    public MarryCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/736281363464060979/736281372750118912/marry.gif",
                "https://cdn.discordapp.com/attachments/736281363464060979/736281376898154546/marry.gif",
                "https://cdn.discordapp.com/attachments/736281363464060979/736281381692244048/marry.gif",
                "https://cdn.discordapp.com/attachments/736281363464060979/736281396355530812/marry.gif",
                "https://cdn.discordapp.com/attachments/736281363464060979/736281399065051137/marry.gif",
                "https://cdn.discordapp.com/attachments/736281363464060979/736281405088202782/marry.gif"
        );
    }

}
