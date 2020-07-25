package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;
import Commands.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "yaoifuck",
        emoji = "\uD83D\uDC68\uD83D\uDECF\uD83D\uDC68️",
        executable = true,
        nsfw = true
)
public class YaoiFuckCommand extends InteractionAbstract {

    public YaoiFuckCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736282657062912080/736282666126540870/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736282674771001514/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736282683608662021/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736282721147683077/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736282730442129498/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736282734363803758/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736282742261547120/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736282750390239425/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736282759458193438/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736282769197629490/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736282776311169034/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736282784410370148/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736282790647169085/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736282865708564560/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736282873144803418/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736282877402021888/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736282999921967134/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736283005601054860/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736283034310934608/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736283038224351272/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736283045077975081/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736283049439789066/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736283053487292446/yaoifuck.gif",
                "https://media.discordapp.net/attachments/736282657062912080/736283058642223154/yaoifuck.gif"
        };
    }

}
