package commands.commandslots.interactionscategory;
import commands.commandlisteners.CommandProperties;
import commands.commandslots.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "yaoicuddle",
        emoji = "\uD83D\uDC68\uD83D\uDC50\uD83D\uDC68",
        executable = true,
        aliases = {"yaoisnuggle"}
)
public class YaoiCuddleCommand extends InteractionAbstract {

    public YaoiCuddleCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736279874452455454/736279883776262144/yaoicuddle.gif",
                "https://media.discordapp.net/attachments/736279874452455454/736279889224925214/yaoicuddle.gif",
                "https://media.discordapp.net/attachments/736279874452455454/736279892043497572/yaoicuddle.gif",
                "https://media.discordapp.net/attachments/736279874452455454/736279896518557787/yaoicuddle.gif",
                "https://media.discordapp.net/attachments/736279874452455454/736279903036506202/yaoicuddle.gif",
                "https://media.discordapp.net/attachments/736279874452455454/736279906564178000/yaoicuddle.gif",
                "https://media.discordapp.net/attachments/736279874452455454/736279911534428271/yaoicuddle.gif",
                "https://media.discordapp.net/attachments/736279874452455454/736279914172645556/yaoicuddle.gif",
                "https://media.discordapp.net/attachments/736279874452455454/736279917976617020/yaoicuddle.gif",
                "https://media.discordapp.net/attachments/736279874452455454/736279927313268786/yaoicuddle.gif",
                "https://media.discordapp.net/attachments/736279874452455454/736279930203013270/yaoicuddle.gif",
                "https://media.discordapp.net/attachments/736279874452455454/736279935152554044/yaoicuddle.gif",
                "https://media.discordapp.net/attachments/736279874452455454/736279946741153882/yaoicuddle.gif"
        };
    }

}
