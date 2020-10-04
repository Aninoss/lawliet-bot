package commands.runnables.emotescategory;

import commands.listeners.CommandProperties;
import commands.runnables.EmoteAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "sip",
        emoji = "☕",
        executable = true,
        aliases = {"teasip", "tea"}
)
public class SipCommand extends EmoteAbstract {

    public SipCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/761612990784471041/761613273048678430/sip.gif",
                "https://media.discordapp.net/attachments/761612990784471041/761613300508262420/sip.gif",
                "https://media.discordapp.net/attachments/761612990784471041/761613356704333847/sip.gif",
                "https://media.discordapp.net/attachments/761612990784471041/761613393864949760/sip.gif",
                "https://media.discordapp.net/attachments/761612990784471041/761613430808510464/sip.gif",
                "https://media.discordapp.net/attachments/761612990784471041/761613481614114826/sip.gif",
                "https://media.discordapp.net/attachments/761612990784471041/761613511892795473/sip.gif",
                "https://media.discordapp.net/attachments/761612990784471041/761613569317666886/sip.gif",
                "https://media.discordapp.net/attachments/761612990784471041/761620581787238410/sip.gif",
                "https://media.discordapp.net/attachments/761612990784471041/761613660744712252/sip.gif",
                "https://media.discordapp.net/attachments/761612990784471041/762355934864474112/sip.gif",
                "https://media.discordapp.net/attachments/761612990784471041/762358715964850176/sip.gif"
        };
    }

}
