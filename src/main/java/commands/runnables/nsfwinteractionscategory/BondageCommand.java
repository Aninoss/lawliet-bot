package commands.runnables.nsfwinteractionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "bondage",
        emoji = "🪢",
        executableWithoutArgs = true,
        nsfw = true,
        requiresFullMemberCache = true,
        aliases = "gag"
)
public class BondageCommand extends RolePlayAbstract {

    public BondageCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true,
                "https://cdn.discordapp.com/attachments/992100317430808596/992100428948979812/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992100462910251138/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992100531730399232/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992100561044373504/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992100598008791151/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992100634566332536/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992101619682529340/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992101666738413668/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992101757272473641/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992101812880551986/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992101961530884106/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992101997643841596/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992102032339120198/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992102066786934914/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992102158830927932/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992102227747536906/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992102275608752288/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992102373361205328/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992102396731859064/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992102611161452627/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992102694632300656/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992102732100010094/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992102795694055424/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992102840464048168/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992102896101503057/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992102941173485639/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992103003421155369/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992103053899612170/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992103090830454804/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992103134992269392/bondage.gif",
                "https://cdn.discordapp.com/attachments/992100317430808596/992103175530229840/bondage.gif"
        );
    }

}
