package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.InteractionAbstract;

@CommandProperties(
        trigger = "massage",
        emoji = "\uD83D\uDC86",
        executableWithoutArgs = true
)
public class MassageCommand extends InteractionAbstract {

    public MassageCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736276388226662420/736276394828627968/massage.gif",
                "https://media.discordapp.net/attachments/736276388226662420/736276400524492950/massage.gif",
                "https://media.discordapp.net/attachments/736276388226662420/736276410762526752/massage.gif",
                "https://media.discordapp.net/attachments/736276388226662420/736276423819526174/massage.gif",
                "https://media.discordapp.net/attachments/736276388226662420/736276427174969354/massage.gif"
        };
    }

}
