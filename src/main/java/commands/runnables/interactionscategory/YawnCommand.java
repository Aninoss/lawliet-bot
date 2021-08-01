package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "yawn",
        emoji = "\uD83E\uDD71",
        executableWithoutArgs = true,
        requiresMemberCache = true,
        aliases = { "wakeup", "tired", "sleepy" }
)
public class YawnCommand extends RolePlayAbstract {

    public YawnCommand(Locale locale, String prefix) {
        super(locale, prefix, false,
                "https://cdn.discordapp.com/attachments/736262435501113465/736262443247992832/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262449401167889/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262460868395029/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262468191649873/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262480732356618/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262495081070592/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262507760451645/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262515029442721/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262522956414996/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262526651596910/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262535459897394/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262543294857286/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262549422604349/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262557404495872/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262561984544828/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262569668640838/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262578526748733/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262584621072414/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262590606344242/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262595928916048/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262603688640554/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262612383301642/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262624517423224/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262631064731668/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/736262644293697546/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/741228041199681546/yawn.gif",
                "https://cdn.discordapp.com/attachments/736262435501113465/815214009128976424/yawn.gif"
        );
    }

}
