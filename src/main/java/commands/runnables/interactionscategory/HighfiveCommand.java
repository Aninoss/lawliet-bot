package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "highfive",
        emoji = "✋",
        executableWithoutArgs = true,
        requiresFullMemberCache = true
)
public class HighfiveCommand extends RolePlayAbstract {

    public HighfiveCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true);

        setFtfGifs(
                "https://cdn.discordapp.com/attachments/736274901673181216/736274922028138606/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/736275017297428511/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/736275049727787029/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/736275057722130462/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/736275096833884261/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/834508220034711552/highfive.gif"
        );
        setFtmGifs(
                "https://cdn.discordapp.com/attachments/736274901673181216/736274914381922444/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/736274931934953582/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/736274985558999131/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/736275014097305660/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/736275063833100288/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/736275079192903851/highfive.gif"
        );
        setFtaGifs(
                "https://cdn.discordapp.com/attachments/736274901673181216/736274917502353408/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/736274955003494410/highfive.gif"
        );
        setMtmGifs(
                "https://cdn.discordapp.com/attachments/736274901673181216/736274948129030144/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/736274966286172225/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/736274973488054313/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/736274978973941891/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/736274994304122960/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/736275023697936425/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/736275028873576478/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/736275037639802952/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/736275069109796894/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/736275088323903558/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/834508141907279932/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/834508156146810980/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/834508196030316654/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/881895480227938334/highfive.gif"
        );
        setMtaGifs(
                "https://cdn.discordapp.com/attachments/736274901673181216/736275073866006568/highfive.gif"
        );
        setAtaGifs(
                "https://cdn.discordapp.com/attachments/736274901673181216/736274926260060251/highfive.gif",
                "https://cdn.discordapp.com/attachments/736274901673181216/834508208038608906/highfive.gif"
        );
    }

}
