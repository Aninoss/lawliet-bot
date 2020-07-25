package Commands.EmotesCategory;
import CommandListeners.CommandProperties;
import Commands.EmoteAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "facepalm",
        emoji = "\uD83E\uDD26",
        executable = true
)
public class FacepalmCommand extends EmoteAbstract {

    public FacepalmCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736262744474517584/736262755283238992/facepalm.gif",
                "https://media.discordapp.net/attachments/736262744474517584/736262759964213439/facepalm.gif",
                "https://media.discordapp.net/attachments/736262744474517584/736262764284346428/facepalm.gif",
                "https://media.discordapp.net/attachments/736262744474517584/736262769518837880/facepalm.gif",
                "https://media.discordapp.net/attachments/736262744474517584/736262779408744509/facepalm.gif",
                "https://media.discordapp.net/attachments/736262744474517584/736262784811270273/facepalm.gif",
                "https://media.discordapp.net/attachments/736262744474517584/736262794105716796/facepalm.gif",
                "https://media.discordapp.net/attachments/736262744474517584/736262798262403162/facepalm.gif",
                "https://media.discordapp.net/attachments/736262744474517584/736262804683751504/facepalm.gif",
                "https://media.discordapp.net/attachments/736262744474517584/736262815425495142/facepalm.gif",
                "https://media.discordapp.net/attachments/736262744474517584/736262819971858543/facepalm.gif",
                "https://media.discordapp.net/attachments/736262744474517584/736262826171039855/facepalm.gif",
                "https://media.discordapp.net/attachments/736262744474517584/736262831544074250/facepalm.gif",
                "https://media.discordapp.net/attachments/736262744474517584/736262835557892126/facepalm.gif",
                "https://media.discordapp.net/attachments/736262744474517584/736262842528825375/facepalm.gif",
                "https://media.discordapp.net/attachments/736262744474517584/736262847973294171/facepalm.gif",
                "https://media.discordapp.net/attachments/736262744474517584/736262854734512188/facepalm.gif",
                "https://media.discordapp.net/attachments/736262744474517584/736262859821940836/facepalm.gif",
                "https://media.discordapp.net/attachments/736262744474517584/736262871125721209/facepalm.gif"
        };
    }

}
