package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "yaoihug",
        emoji = "\uD83D\uDC68\uD83D\uDC50\uD83D\uDC68",
        executableWithoutArgs = true,
        requiresFullMemberCache = true
)
public class YaoiHugCommand extends RolePlayAbstract {

    public YaoiHugCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/736278340733894710/736278344986918954/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278350959607949/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278355015630868/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278358869934101/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278366818271302/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278374993100840/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278379849973780/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278403434676224/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278410568925315/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278429510533176/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278434866790532/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/741227684587241503/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/834717466424901652/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/877645631051665519/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/881896699277238282/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/1150478633740271687/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/1334543487915524247/yaoihug.gif"
        );
    }

}
