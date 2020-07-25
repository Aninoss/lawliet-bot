package Commands.InteractionsCategory;

import CommandListeners.CommandProperties;
import Commands.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "wave",
        emoji = "\uD83D\uDC4B",
        executable = true,
        aliases = {"greet", "bye", "hi", "cya"}
)
public class WaveCommand extends InteractionAbstract {

    public WaveCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736274581492465665/736274586097811500/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274589025566740/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274593622523954/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274598294978660/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274600819818576/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274608063512707/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274612731642017/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274618465386577/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274622391124098/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274626031779850/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274631316734064/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274633573007452/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274635900977212/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274639722119198/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274644025475213/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274656981418065/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274668524273734/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274679093919834/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274684873670736/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274690531917934/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274693182455849/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274697863561276/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274706163826800/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274711633461318/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274716754575380/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274730012770324/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274736010493992/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274738392989746/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274744718000240/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274750862655588/wave.gif",
                "https://media.discordapp.net/attachments/736274581492465665/736274757221351494/wave.gif"
        };
    }

}
