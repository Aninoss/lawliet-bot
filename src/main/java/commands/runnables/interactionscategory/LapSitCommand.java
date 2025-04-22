package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "lapsit",
        emoji = "🪑",
        executableWithoutArgs = true,
        requiresFullMemberCache = true
)
public class LapSitCommand extends RolePlayAbstract {

    public LapSitCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false);

        setFtfGifs(
                "https://cdn.discordapp.com/attachments/969964503330586684/969964606103646238/lapsit.gif",
                "https://cdn.discordapp.com/attachments/969964503330586684/969964638131327017/lapsit.gif",
                "https://cdn.discordapp.com/attachments/969964503330586684/969964846420480080/lapsit.gif"
        );
        setFtmGifs(
                "https://cdn.discordapp.com/attachments/969964503330586684/969964677922697276/lapsit.gif",
                "https://cdn.discordapp.com/attachments/969964503330586684/969964770348392448/lapsit.gif",
                "https://cdn.discordapp.com/attachments/969964503330586684/969964810634674177/lapsit.gif",
                "https://cdn.discordapp.com/attachments/969964503330586684/969964927680917585/lapsit.gif"
        );
        setFtaGifs(
                "https://cdn.discordapp.com/attachments/969964503330586684/969964724311687178/lapsit.gif",
                "https://cdn.discordapp.com/attachments/969964503330586684/969964883946905600/lapsit.gif"
        );
        setAtfGifs(
                "https://cdn.discordapp.com/attachments/969964503330586684/969964949399040000/lapsit.gif"
        );
    }

}
