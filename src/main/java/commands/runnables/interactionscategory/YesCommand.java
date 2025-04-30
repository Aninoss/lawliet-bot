package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "yes",
        emoji = "\uD83D\uDC4D",
        executableWithoutArgs = true,
        aliases = { "thumbsup", "thumpsup" }
)
public class YesCommand extends RolePlayAbstract {

    public YesCommand(Locale locale, String prefix) {
        super(locale, prefix, false);

        setFtaGifs(
                "https://cdn.discordapp.com/attachments/736254151620820993/736254158205747270/yes.gif",
                "https://cdn.discordapp.com/attachments/736254151620820993/736254171891761172/yes.gif",
                "https://cdn.discordapp.com/attachments/736254151620820993/736254184617410580/yes.gif",
                "https://cdn.discordapp.com/attachments/736254151620820993/736254202950582302/yes.gif",
                "https://cdn.discordapp.com/attachments/736254151620820993/736254205492461568/yes.gif",
                "https://cdn.discordapp.com/attachments/736254151620820993/736254214275072100/yes.gif",
                "https://cdn.discordapp.com/attachments/736254151620820993/736254228493893632/yes.gif",
                "https://cdn.discordapp.com/attachments/736254151620820993/736254237205331978/yes.gif",
                "https://cdn.discordapp.com/attachments/736254151620820993/736254240372031496/yes.gif",
                "https://cdn.discordapp.com/attachments/736254151620820993/736254251327684698/yes.gif",
                "https://cdn.discordapp.com/attachments/736254151620820993/736254259837796352/yes.gif",
                "https://cdn.discordapp.com/attachments/736254151620820993/736254266213138442/yes.gif"
        );
        setMtaGifs(
                "https://cdn.discordapp.com/attachments/736254151620820993/736254166216998912/yes.gif",
                "https://cdn.discordapp.com/attachments/736254151620820993/736254175394136234/yes.gif",
                "https://cdn.discordapp.com/attachments/736254151620820993/736254179236118558/yes.gif",
                "https://cdn.discordapp.com/attachments/736254151620820993/736254245283561522/yes.gif"
        );
        setAtaGifs(
                "https://cdn.discordapp.com/attachments/736254151620820993/736254160902553661/yes.gif",
                "https://cdn.discordapp.com/attachments/736254151620820993/736254221711835136/yes.gif",
                "https://cdn.discordapp.com/attachments/736254151620820993/736254234567376936/yes.gif"
        );
    }

}
