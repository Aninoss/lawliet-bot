package commands.runnables.emotescategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.EmoteAbstract;

@CommandProperties(
        trigger = "smug",
        emoji = "\uD83D\uDE0F",
        executableWithoutArgs = true
)
public class SmugCommand extends EmoteAbstract {

    public SmugCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[] {
                "https://media.discordapp.net/attachments/736262910678007808/736262917132910602/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736262920786149467/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736262926553579640/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736262937626411108/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736262940654567615/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736262944467452026/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736262946866331838/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736262953417834616/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736262962527862834/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736262971784691843/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736262975224283177/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736262980521426984/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736262987991744579/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736262994715082825/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736262999052124260/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736263001643941928/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736263003573583901/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736263014059212880/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736263019079925831/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736263031767564298/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736263040764477459/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736263046040780820/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736263057927307324/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736263069365305344/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736263075304439808/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736263085173506118/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/736263094984245299/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/745277946721206352/smug.gif",
                "https://media.discordapp.net/attachments/736262910678007808/745278389128003674/smug.gif"
        };
    }

}
