package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "wave",
        emoji = "\uD83D\uDC4B",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "greet", "bye", "hi", "cya" }
)
public class WaveCommand extends RolePlayAbstract {

    public WaveCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/736274581492465665/736274586097811500/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274589025566740/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274593622523954/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274598294978660/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274600819818576/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274608063512707/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274612731642017/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274618465386577/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274622391124098/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274626031779850/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274631316734064/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274633573007452/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274635900977212/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274639722119198/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274644025475213/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274656981418065/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274668524273734/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274679093919834/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274684873670736/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274690531917934/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274693182455849/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274697863561276/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274706163826800/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274711633461318/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274716754575380/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274730012770324/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274736010493992/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274738392989746/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274744718000240/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274750862655588/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/736274757221351494/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834847812989747230/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834847825321263144/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834847837438083113/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834847849199173723/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834847860867465246/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834847872620822538/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834847884151226377/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834847895299555359/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834847906690760724/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834847918044217364/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834847929558761492/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834847943093780538/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834847953994121246/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834847966547411004/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834847979432968212/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834847992917000192/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834848008859942993/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834848019899482182/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834848032960020510/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834848046403682314/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/834848059166425148/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/839471656900755476/wave.gif",
                "https://cdn.discordapp.com/attachments/736274581492465665/954007116157444136/wave.gif"
        );
    }

}
