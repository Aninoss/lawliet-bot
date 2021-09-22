package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "kill",
        emoji = "☠️",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "die" }
)
public class KillCommand extends RolePlayAbstract {

    public KillCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/736270438627278859/736270444973391992/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270452443316224/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270456952193125/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270463218745455/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270466481782824/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270475612782642/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270485796683876/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270490825654312/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270498630991922/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270507510464592/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270516230291506/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270521469239617/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270527290802247/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270533989105684/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270539592695868/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270546278416475/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270552582455356/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270558181851196/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270565878267944/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270570148331680/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270576607428718/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270585058820186/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270613865562163/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270624300728320/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270633260023858/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/736270655749619732/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/834510727943290920/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/834510804056932412/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/834510811983773736/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/834510824755953699/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/834510983585464350/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/881891935055392848/kill.gif",
                "https://cdn.discordapp.com/attachments/736270438627278859/890291977206239293/kill.gif"
        );
    }

}
