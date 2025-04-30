package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "squish",
        emoji = "🍩",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "squeeze" }
)
public class SquishCommand extends RolePlayAbstract {

    public SquishCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false);

        setFtfGifs(
                "https://cdn.discordapp.com/attachments/955386888624148491/955387234524213299/squish.gif",
                "https://cdn.discordapp.com/attachments/955386888624148491/955387465257082890/squish.gif",
                "https://cdn.discordapp.com/attachments/955386888624148491/955387497041502208/squish.gif"
        );
        setFtaGifs(
                "https://cdn.discordapp.com/attachments/955386888624148491/955387385502400522/squish.gif",
                "https://cdn.discordapp.com/attachments/955386888624148491/955387410596892752/squish.gif"
        );
        setMtfGifs(
                "https://cdn.discordapp.com/attachments/955386888624148491/955387215511433226/squish.gif"
        );
        setAtfGifs(
                "https://cdn.discordapp.com/attachments/955386888624148491/955387024976793640/squish.gif",
                "https://cdn.discordapp.com/attachments/955386888624148491/955387363712974848/squish.gif",
                "https://cdn.discordapp.com/attachments/955386888624148491/955387448026873876/squish.gif",
                "https://cdn.discordapp.com/attachments/955386888624148491/955387528523944006/squish.gif"
        );
    }

}
