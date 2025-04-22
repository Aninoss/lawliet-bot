package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "sip",
        emoji = "☕",
        executableWithoutArgs = true,
        aliases = { "teasip", "tea" }
)
public class SipCommand extends RolePlayAbstract {

    public SipCommand(Locale locale, String prefix) {
        super(locale, prefix, false);

        setFtaGifs(
                "https://cdn.discordapp.com/attachments/761612990784471041/761613273048678430/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/761613356704333847/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/761613430808510464/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/761613481614114826/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/761613511892795473/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/761613569317666886/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/761620581787238410/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/761613660744712252/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/762355934864474112/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/834837435015495720/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/834837463930503238/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/834837478748979270/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/834837495506010152/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/1031120861303935116/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/1042439000289452132/sip.gif"
        );
        setMtaGifs(
                "https://cdn.discordapp.com/attachments/761612990784471041/761613393864949760/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/762358715964850176/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/779071115187978310/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/834837508943642714/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/860571962316292096/sip.gif"
        );
    }

}
