package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "poke",
        emoji = "\uD83D\uDC49",
        executableWithoutArgs = true,
        requiresFullMemberCache = true
)
public class PokeCommand extends RolePlayAbstract {

    public PokeCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/736273767520665610/736273774760165506/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/736273784067194890/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/736273788198715522/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/736273794540372018/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/736273799246381187/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/736273806900985922/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/736273816673845338/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/736273820125495440/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/736273822272979036/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/736273824676446249/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/736273834264756254/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/736273837829652490/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/736273843899072633/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/736273846717382768/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/736273852824551486/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/736273856888832070/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/736273864492843048/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/736273867802279987/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/736273880112693348/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/736273886299029535/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/834835419966996540/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/834835432688189450/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/834835446395437066/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/834835460034527242/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/834835471996682280/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/834835483997503528/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/834835495871316044/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/834835507515097151/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/834835519157698560/poke.gif",
                "https://cdn.discordapp.com/attachments/736273767520665610/834835531182768128/poke.gif"
        );
    }

}
