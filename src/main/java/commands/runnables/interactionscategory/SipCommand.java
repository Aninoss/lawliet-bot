package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "sip",
        emoji = "☕",
        executableWithoutArgs = true,
        aliases = { "teasip", "tea", "drink" }
)
public class SipCommand extends RolePlayAbstract {

    public SipCommand(Locale locale, String prefix) {
        super(locale, prefix, false,
                "https://cdn.discordapp.com/attachments/761612990784471041/761613273048678430/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/761613356704333847/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/761613393864949760/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/761613430808510464/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/761613481614114826/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/761613511892795473/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/761613569317666886/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/761620581787238410/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/761613660744712252/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/762355934864474112/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/762358715964850176/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/779071115187978310/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/834837435015495720/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/834837463930503238/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/834837478748979270/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/834837495506010152/sip.gif",
                "https://cdn.discordapp.com/attachments/761612990784471041/834837508943642714/sip.gif"
        );
    }

}
