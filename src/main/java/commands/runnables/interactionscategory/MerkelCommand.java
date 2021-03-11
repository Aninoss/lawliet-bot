package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.InteractionAbstract;

@CommandProperties(
        trigger = "merkel",
        emoji = "\uD83C\uDDE9\uD83C\uDDEA",
        executableWithoutArgs = true
)
public class MerkelCommand extends InteractionAbstract {

    public MerkelCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736269653948760146/736269660143747082/merkel.png"
        };
    }

}
