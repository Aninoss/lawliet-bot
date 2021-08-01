package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "slap",
        emoji = "\uD83D\uDC4F",
        executableWithoutArgs = true,
        requiresMemberCache = true
)
public class SlapCommand extends RolePlayAbstract {

    public SlapCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/736271623098990792/736271626714218507/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/736271635174391979/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/736271639301324860/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/736271643634040923/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/736271649099481129/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/736271662420328538/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/736271669164900403/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/736271673572982874/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/736271676811247626/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/736271684645945364/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/736271689603612772/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/736271694947418152/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/736271701440069642/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/736271717617369198/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/736271734872866887/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/736271742724472872/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/736271751725580320/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/736271759279521903/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/834837732924325929/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/834837745754964008/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/834837756680863845/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/834837768458469436/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/834837780059258910/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/834837791942770698/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/834837803761795132/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/834837815417634866/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/834837830919782431/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/834837844664123462/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/834837861491802203/slap.gif",
                "https://cdn.discordapp.com/attachments/736271623098990792/868914264692363304/slap.gif"
        );
    }

}
