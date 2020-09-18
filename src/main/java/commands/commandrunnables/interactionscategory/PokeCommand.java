package commands.commandrunnables.interactionscategory;
import commands.commandlisteners.CommandProperties;
import commands.commandrunnables.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "poke",
        emoji = "\uD83D\uDC49",
        executable = true
)
public class PokeCommand extends InteractionAbstract {

    public PokeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736273767520665610/736273774760165506/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273784067194890/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273788198715522/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273794540372018/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273799246381187/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273803910316142/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273806900985922/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273816673845338/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273820125495440/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273822272979036/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273824676446249/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273834264756254/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273837829652490/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273843899072633/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273846717382768/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273852824551486/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273856888832070/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273864492843048/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273867802279987/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273880112693348/poke.gif",
                "https://media.discordapp.net/attachments/736273767520665610/736273886299029535/poke.gif"
        };
    }

}
