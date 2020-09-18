package commands.commandslots.interactionscategory;
import commands.commandlisteners.CommandProperties;
import commands.commandslots.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "merkel",
        emoji = "\uD83C\uDDE9\uD83C\uDDEA",
        executable = true
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
