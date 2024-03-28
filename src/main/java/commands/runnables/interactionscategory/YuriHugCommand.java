package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "yurihug",
        emoji = "\uD83D\uDC69\uD83D\uDC50\uD83D\uDC69",
        executableWithoutArgs = true,
        requiresFullMemberCache = true
)
public class YuriHugCommand extends RolePlayAbstract {

    public YuriHugCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/736278804829306930/736278809795625080/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736278814065426573/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736278820256088094/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736278846114103316/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736278854955565066/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736278911939379345/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736278978184347759/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736279020110479480/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736279287572725821/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736279360092373102/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736279429419892776/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/834849513579216956/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/834849563474657320/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/834849577344434196/yurihug.gif"
        );
    }

}
