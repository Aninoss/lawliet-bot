package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.InteractionAbstract;

@CommandProperties(
        trigger = "bonk",
        emoji = "🏏",
        executableWithoutArgs = true,
        aliases = { "lewd", "hornyjail", "horny", "jail" }
)
public class BonkCommand extends InteractionAbstract {

    public BonkCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[] {
                "https://cdn.discordapp.com/attachments/826095943748943913/826095979614044180/bonk.gif"
        };
    }

}
