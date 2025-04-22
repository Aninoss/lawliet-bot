package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "baka",
        emoji = "\uD83D\uDCA2",
        executableWithoutArgs = true,
        requiresFullMemberCache = true
)
public class BakaCommand extends RolePlayAbstract {

    public BakaCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false);

        setFtmGifs(
                "https://cdn.discordapp.com/attachments/736271836278423553/736271868557787146/baka.gif",
                "https://cdn.discordapp.com/attachments/736271836278423553/736271901420159007/baka.gif"
        );
        setFtaGifs(
                "https://cdn.discordapp.com/attachments/736271836278423553/736271843027058788/baka.gif",
                "https://cdn.discordapp.com/attachments/736271836278423553/736271848546893975/baka.gif",
                "https://cdn.discordapp.com/attachments/736271836278423553/736271857443012730/baka.gif",
                "https://cdn.discordapp.com/attachments/736271836278423553/736271863457775656/baka.gif",
                "https://cdn.discordapp.com/attachments/736271836278423553/736271876283695264/baka.gif",
                "https://cdn.discordapp.com/attachments/736271836278423553/736271894315139123/baka.gif"
        );
        setMtfGifs(
                "https://cdn.discordapp.com/attachments/736271836278423553/736271881627500593/baka.gif"
        );
        setMtmGifs(
                "https://cdn.discordapp.com/attachments/736271836278423553/736271906398797824/baka.gif"
        );
    }

}
