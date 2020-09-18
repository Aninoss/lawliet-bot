package commands.commandrunnables.interactionscategory;
import commands.commandlisteners.CommandProperties;
import commands.commandrunnables.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "marry",
        emoji = "\uD83D\uDC8D",
        executable = true
)
public class MarryCommand extends InteractionAbstract {

    public MarryCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736281363464060979/736281372750118912/marry.gif",
                "https://media.discordapp.net/attachments/736281363464060979/736281376898154546/marry.gif",
                "https://media.discordapp.net/attachments/736281363464060979/736281381692244048/marry.gif",
                "https://media.discordapp.net/attachments/736281363464060979/736281396355530812/marry.gif",
                "https://media.discordapp.net/attachments/736281363464060979/736281399065051137/marry.gif",
                "https://media.discordapp.net/attachments/736281363464060979/736281405088202782/marry.gif"
        };
    }

}
