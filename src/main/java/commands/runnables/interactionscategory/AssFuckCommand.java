package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "assfuck",
        emoji = "🍑",
        executableWithoutArgs = true,
        nsfw = true,
        requiresFullMemberCache = true,
        aliases = "fuckass"
)
public class AssFuckCommand extends RolePlayAbstract {

    public AssFuckCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true,
                "https://cdn.discordapp.com/attachments/965979549533872319/965979631041789993/assfuck.gif",
                "https://cdn.discordapp.com/attachments/965979549533872319/965979664352968714/assfuck.gif",
                "https://cdn.discordapp.com/attachments/965979549533872319/965979778542878750/assfuck.gif",
                "https://cdn.discordapp.com/attachments/965979549533872319/965979800223227984/assfuck.gif",
                "https://cdn.discordapp.com/attachments/965979549533872319/965979846381559838/assfuck.gif",
                "https://cdn.discordapp.com/attachments/965979549533872319/965979877922717746/assfuck.gif",
                "https://cdn.discordapp.com/attachments/965979549533872319/965979902828511272/assfuck.gif",
                "https://cdn.discordapp.com/attachments/965979549533872319/965980336204939294/assfuck.gif",
                "https://cdn.discordapp.com/attachments/965979549533872319/965980358581563402/assfuck.gif",
                "https://cdn.discordapp.com/attachments/965979549533872319/965981047575699476/assfuck.gif"
        );
    }

}
