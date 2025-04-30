package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "awkward",
        emoji = "\uD83D\uDE05",
        executableWithoutArgs = true
)
public class AwkwardCommand extends RolePlayAbstract {

    public AwkwardCommand(Locale locale, String prefix) {
        super(locale, prefix, false);

        setFtaGifs(
                "https://cdn.discordapp.com/attachments/736253855339249695/736253858308817007/awkward.gif",
                "https://cdn.discordapp.com/attachments/736253855339249695/736253865015377960/awkward.gif",
                "https://cdn.discordapp.com/attachments/736253855339249695/736253876914880542/awkward.gif",
                "https://cdn.discordapp.com/attachments/736253855339249695/736253887203377172/awkward.gif",
                "https://cdn.discordapp.com/attachments/736253855339249695/736253900210044929/awkward.gif",
                "https://cdn.discordapp.com/attachments/736253855339249695/736253915129053184/awkward.gif",
                "https://cdn.discordapp.com/attachments/736253855339249695/736253920858603580/awkward.gif",
                "https://cdn.discordapp.com/attachments/736253855339249695/736253931860000879/awkward.gif",
                "https://cdn.discordapp.com/attachments/736253855339249695/736253935895183430/awkward.gif",
                "https://cdn.discordapp.com/attachments/736253855339249695/736253958380585080/awkward.gif"
        );
        setMtaGifs(
                "https://cdn.discordapp.com/attachments/736253855339249695/736253872057745539/awkward.gif",
                "https://cdn.discordapp.com/attachments/736253855339249695/736253895185268797/awkward.gif",
                "https://cdn.discordapp.com/attachments/736253855339249695/736253908279623731/awkward.gif",
                "https://cdn.discordapp.com/attachments/736253855339249695/736253940957577226/awkward.gif",
                "https://cdn.discordapp.com/attachments/736253855339249695/736253952143655032/awkward.gif"
        );
        setAtaGifs(
                "https://cdn.discordapp.com/attachments/736253855339249695/736253927963492512/awkward.gif"
        );
    }

}
