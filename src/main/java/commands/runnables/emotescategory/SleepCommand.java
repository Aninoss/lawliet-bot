package commands.runnables.emotescategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.EmoteAbstract;

@CommandProperties(
    trigger = "sleep",
    emoji = "\uD83D\uDCA4",
    executableWithoutArgs = true
)
public class SleepCommand extends EmoteAbstract {

    public SleepCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736261025166524498/736261034935058563/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261044783153222/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261050495926303/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261058494333008/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261070666203158/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261078563946618/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261089083392020/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261105264885770/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261117239885854/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261126576144394/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261135648555098/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261138173526076/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261144829755452/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261150777540708/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261165151420486/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261177361039462/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261183094521856/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261210546372688/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261216426524753/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261224064483338/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261237263826964/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261246642552912/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261270105358396/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261278250565662/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261283615342672/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261293211648110/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261303915642880/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261314514780230/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261332067942400/sleep.gif",
                "https://media.discordapp.net/attachments/736261025166524498/736261433293144085/sleep.gif"
        };
    }

}
