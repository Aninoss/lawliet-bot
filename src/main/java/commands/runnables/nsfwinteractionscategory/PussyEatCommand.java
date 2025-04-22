package commands.runnables.nsfwinteractionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

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
        super(locale, prefix, true, false);

        setFtfGifs(
                "https://cdn.discordapp.com/attachments/964484282984857610/964484953062670336/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/964484985551740948/pussyeat.gif"
        );
        setMtfGifs(
                "https://cdn.discordapp.com/attachments/964484282984857610/964484568642125854/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/964484607351357521/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/964484793255460934/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/964484837744476180/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/964484887572774972/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/964484920548401192/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/964485018485399622/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/964485051599446016/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/1054775394622001182/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/1054775458006306986/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/1054775595931795486/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/1054775652936601620/pussyeat.gif",
                "https://cdn.discordapp.com/attachments/964484282984857610/1054775911792246845/pussyeat.gif"
        );
        setAtfGifs(
                "https://cdn.discordapp.com/attachments/964484282984857610/1054775530748133486/pussyeat.gif"
        );
    }

}
