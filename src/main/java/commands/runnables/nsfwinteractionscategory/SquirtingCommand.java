package commands.runnables.nsfwinteractionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "squirting",
        emoji = "💦",
        executableWithoutArgs = true,
        nsfw = true,
        aliases = "squirt",
        requiresFullMemberCache = true
)
public class SquirtingCommand extends RolePlayAbstract {

    public SquirtingCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/1331988926834933872/1331988979528237067/squirt.gif",
                "https://cdn.discordapp.com/attachments/1331988926834933872/1331989045588525078/squirt.gif",
                "https://cdn.discordapp.com/attachments/1331988926834933872/1331989125468913724/squirt.gif",
                "https://cdn.discordapp.com/attachments/1331988926834933872/1331989195266330644/squirt.gif",
                "https://cdn.discordapp.com/attachments/1331988926834933872/1331989763401453620/squirt.gif",
                "https://cdn.discordapp.com/attachments/1331988926834933872/1331989854258462852/squirt.gif",
                "https://cdn.discordapp.com/attachments/1331988926834933872/1331989927197544581/squirt.gif",
                "https://cdn.discordapp.com/attachments/1331988926834933872/1331989997242286092/squirt.gif",
                "https://cdn.discordapp.com/attachments/1331988926834933872/1331990056977694760/squirt.gif",
                "https://cdn.discordapp.com/attachments/1331988926834933872/1331990109079339009/squirt.gif"
        );
    }

}
