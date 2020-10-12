package commands.runnables.emotescategory;
import commands.listeners.CommandProperties;
import commands.runnables.EmoteAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "dab",
        emoji = "\uD83C\uDD92",
        executableWithoutArgs = true,
        aliases = {"dap"}
)
public class DabCommand extends EmoteAbstract {

    public DabCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736253507618865215/736253511095943269/dab.gif",
                "https://media.discordapp.net/attachments/736253507618865215/736253518100299796/dab.gif",
                "https://media.discordapp.net/attachments/736253507618865215/736253522198396928/dab.gif",
        };
    }

}
