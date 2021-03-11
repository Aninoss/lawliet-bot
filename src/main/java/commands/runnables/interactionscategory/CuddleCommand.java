package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.InteractionAbstract;

@CommandProperties(
        trigger = "cuddle",
        emoji = "\uD83D\uDC50",
        executableWithoutArgs = true,
        aliases = {"snuggle"}
)
public class CuddleCommand extends InteractionAbstract {

    public CuddleCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736279549121396816/736279555710386306/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279565739229275/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279573867659394/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279582050877530/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279596345065572/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279602934317187/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279609166790757/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279617392083005/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279622240567447/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279634341003335/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279637302312991/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279643195310090/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279649851539532/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279659590844436/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279668340162660/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279681246167130/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279685998182420/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279695058010112/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279713752023392/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279725562921011/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279735188848672/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279744655523850/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279753585328218/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279758425554984/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279761495785512/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279766386212954/cuddle.gif",
                "https://media.discordapp.net/attachments/736279549121396816/736279770983301120/cuddle.gif"
        };
    }

}
