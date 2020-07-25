package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;
import Commands.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "yurikiss",
        emoji = "\uD83D\uDC69\u200D❤️\u200D\uD83D\uDC69",
        executable = true
)
public class YuriKissCommand extends InteractionAbstract {

    public YuriKissCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736281091534618674/736281096764784780/yurikiss.gif",
                "https://media.discordapp.net/attachments/736281091534618674/736281104201285732/yurikiss.gif",
                "https://media.discordapp.net/attachments/736281091534618674/736281115098087506/yurikiss.gif",
                "https://media.discordapp.net/attachments/736281091534618674/736281124774346822/yurikiss.gif",
                "https://media.discordapp.net/attachments/736281091534618674/736281129346400416/yurikiss.gif",
                "https://media.discordapp.net/attachments/736281091534618674/736281133649756273/yurikiss.gif",
                "https://media.discordapp.net/attachments/736281091534618674/736281140683604080/yurikiss.gif",
                "https://media.discordapp.net/attachments/736281091534618674/736281151748047088/yurikiss.gif",
                "https://media.discordapp.net/attachments/736281091534618674/736281161139093544/yurikiss.gif",
                "https://media.discordapp.net/attachments/736281091534618674/736281165736181860/yurikiss.gif",
                "https://media.discordapp.net/attachments/736281091534618674/736281173185265794/yurikiss.gif",
                "https://media.discordapp.net/attachments/736281091534618674/736281183738003456/yurikiss.gif",
                "https://media.discordapp.net/attachments/736281091534618674/736281204617248848/yurikiss.gif"
        };
    }

}
