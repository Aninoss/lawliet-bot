package commands.runnables.nsfwinteractionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "pussyeat",
        emoji = "👅",
        executableWithoutArgs = true,
        nsfw = true,
        requiresFullMemberCache = true,
        aliases = "eatpussy"
)
public class PussyEatCommand extends RolePlayAbstract {

    public PussyEatCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true,
                "https://cdn.discordapp.com/attachments/964484282984857610/964484568642125854/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/964484607351357521/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/964484793255460934/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/964484837744476180/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/964484887572774972/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/964484920548401192/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/964484953062670336/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/964484985551740948/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/964485018485399622/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/964485051599446016/pussyeat.gif"
        );
    }

}
