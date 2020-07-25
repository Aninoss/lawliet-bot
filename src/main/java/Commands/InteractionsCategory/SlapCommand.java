package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;
import Commands.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "slap",
        emoji = "\uD83D\uDC4F",
        executable = false
)
public class SlapCommand extends InteractionAbstract {

    public SlapCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736271623098990792/736271626714218507/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271635174391979/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271639301324860/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271643634040923/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271646125588540/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271649099481129/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271652865966143/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271662420328538/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271665884823653/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271669164900403/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271673572982874/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271676811247626/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271684645945364/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271689603612772/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271694947418152/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271701440069642/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271706020249651/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271709446864917/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271717617369198/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271723665555516/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271734872866887/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271742724472872/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271751725580320/slap.gif",
                "https://media.discordapp.net/attachments/736271623098990792/736271759279521903/slap.gif"
        };
    }

}
