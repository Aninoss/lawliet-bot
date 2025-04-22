package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "massage",
        emoji = "\uD83D\uDC86",
        executableWithoutArgs = true,
        requiresFullMemberCache = true
)
public class MassageCommand extends RolePlayAbstract {

    public MassageCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false);

        setMtmGifs(
                "https://cdn.discordapp.com/attachments/736276388226662420/736276423819526174/massage.gif"
        );
        setAtfGifs(
                "https://cdn.discordapp.com/attachments/736276388226662420/736276410762526752/massage.gif"
        );
        setAtmGifs(
                "https://cdn.discordapp.com/attachments/736276388226662420/736276400524492950/massage.gif"
        );
        setAtaGifs(
                "https://cdn.discordapp.com/attachments/736276388226662420/736276394828627968/massage.gif"
        );
    }

}
