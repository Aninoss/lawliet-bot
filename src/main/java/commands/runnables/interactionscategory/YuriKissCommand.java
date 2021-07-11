package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "yurikiss",
        emoji = "\uD83D\uDC69\u200D❤️\u200D\uD83D\uDC69",
        executableWithoutArgs = true
)
public class YuriKissCommand extends RolePlayAbstract {

    public YuriKissCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/736281091534618674/736281096764784780/yurikiss.gif",
                "https://cdn.discordapp.com/attachments/736281091534618674/736281104201285732/yurikiss.gif",
                "https://cdn.discordapp.com/attachments/736281091534618674/736281115098087506/yurikiss.gif",
                "https://cdn.discordapp.com/attachments/736281091534618674/736281124774346822/yurikiss.gif",
                "https://cdn.discordapp.com/attachments/736281091534618674/736281133649756273/yurikiss.gif",
                "https://cdn.discordapp.com/attachments/736281091534618674/736281140683604080/yurikiss.gif",
                "https://cdn.discordapp.com/attachments/736281091534618674/736281151748047088/yurikiss.gif",
                "https://cdn.discordapp.com/attachments/736281091534618674/736281161139093544/yurikiss.gif",
                "https://cdn.discordapp.com/attachments/736281091534618674/736281165736181860/yurikiss.gif",
                "https://cdn.discordapp.com/attachments/736281091534618674/736281173185265794/yurikiss.gif",
                "https://cdn.discordapp.com/attachments/736281091534618674/736281183738003456/yurikiss.gif",
                "https://cdn.discordapp.com/attachments/736281091534618674/736281204617248848/yurikiss.gif",
                "https://cdn.discordapp.com/attachments/736281091534618674/834849779824853023/yurikiss.gif",
                "https://cdn.discordapp.com/attachments/736281091534618674/834849792659947520/yurikiss.gif",
                "https://cdn.discordapp.com/attachments/736281091534618674/834849805246660608/yurikiss.gif",
                "https://cdn.discordapp.com/attachments/736281091534618674/834849818278887435/yurikiss.gif",
                "https://cdn.discordapp.com/attachments/736281091534618674/834849832022704158/yurikiss.gif",
                "https://cdn.discordapp.com/attachments/736281091534618674/863573727069077545/yurikiss.gif"
        );
    }

}
