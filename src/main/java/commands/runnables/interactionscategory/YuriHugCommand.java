package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.InteractionAbstract;

@CommandProperties(
        trigger = "yurihug",
        emoji = "\uD83D\uDC69\uD83D\uDC50\uD83D\uDC69",
        executableWithoutArgs = true
)
public class YuriHugCommand extends InteractionAbstract {

    public YuriHugCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[] {
                "https://media.discordapp.net/attachments/736278804829306930/736278809795625080/yurihug.gif",
                "https://media.discordapp.net/attachments/736278804829306930/736278814065426573/yurihug.gif",
                "https://media.discordapp.net/attachments/736278804829306930/736278820256088094/yurihug.gif",
                "https://media.discordapp.net/attachments/736278804829306930/736278846114103316/yurihug.gif",
                "https://media.discordapp.net/attachments/736278804829306930/736278854955565066/yurihug.gif",
                "https://media.discordapp.net/attachments/736278804829306930/736278911939379345/yurihug.gif",
                "https://media.discordapp.net/attachments/736278804829306930/736278978184347759/yurihug.gif",
                "https://media.discordapp.net/attachments/736278804829306930/736279020110479480/yurihug.gif",
                "https://media.discordapp.net/attachments/736278804829306930/736279287572725821/yurihug.gif",
                "https://media.discordapp.net/attachments/736278804829306930/736279360092373102/yurihug.gif",
                "https://media.discordapp.net/attachments/736278804829306930/736279429419892776/yurihug.gif"
        };
    }

}
