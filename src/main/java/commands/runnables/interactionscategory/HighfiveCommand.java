package commands.runnables.interactionscategory;
import commands.listeners.CommandProperties;
import commands.runnables.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "highfive",
        emoji = "✋",
        executable = true
)
public class HighfiveCommand extends InteractionAbstract {

    public HighfiveCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736274901673181216/736274910271373412/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736274914381922444/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736274917502353408/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736274922028138606/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736274926260060251/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736274931934953582/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736274943276220416/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736274948129030144/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736274955003494410/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736274966286172225/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736274973488054313/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736274978973941891/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736274985558999131/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736274994304122960/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736275005649846372/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736275014097305660/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736275017297428511/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736275023697936425/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736275028873576478/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736275037639802952/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736275049727787029/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736275057722130462/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736275063833100288/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736275069109796894/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736275073866006568/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736275079192903851/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736275088323903558/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736275096833884261/highfive.gif",
                "https://media.discordapp.net/attachments/736274901673181216/736275103364677702/highfive.gif"
        };
    }

}
