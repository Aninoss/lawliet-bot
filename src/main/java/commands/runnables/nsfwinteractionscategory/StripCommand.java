package commands.runnables.nsfwinteractionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "strip",
        emoji = "💈",
        executableWithoutArgs = true,
        nsfw = true,
        requiresFullMemberCache = true,
        aliases = "undress"
)
public class StripCommand extends RolePlayAbstract {

    public StripCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true,
                "https://cdn.discordapp.com/attachments/991784316881346601/991784604409266346/strip.gif",
                "https://cdn.discordapp.com/attachments/991784316881346601/991784967526944898/strip.gif",
                "https://cdn.discordapp.com/attachments/991784316881346601/991785174650077314/strip.gif",
                "https://cdn.discordapp.com/attachments/991784316881346601/991785592281125035/strip.gif",
                "https://cdn.discordapp.com/attachments/991784316881346601/991785635851538482/strip.gif",
                "https://cdn.discordapp.com/attachments/991784316881346601/991785726549180466/strip.gif",
                "https://cdn.discordapp.com/attachments/991784316881346601/991785735772459008/strip.gif",
                "https://cdn.discordapp.com/attachments/991784316881346601/991785912402968667/strip.gif",
                "https://cdn.discordapp.com/attachments/991784316881346601/991786090379882637/strip.gif",
                "https://cdn.discordapp.com/attachments/991784316881346601/991786363856879686/strip.gif",
                "https://cdn.discordapp.com/attachments/991784316881346601/991786585706221688/strip.gif",
                "https://cdn.discordapp.com/attachments/991784316881346601/991786604555423806/strip.gif",
                "https://cdn.discordapp.com/attachments/991784316881346601/991786888086179961/strip.gif",
                "https://cdn.discordapp.com/attachments/991784316881346601/991787148158173315/strip.gif",
                "https://cdn.discordapp.com/attachments/991784316881346601/991787181708423278/strip.gif",
                "https://cdn.discordapp.com/attachments/991784316881346601/991787453683879967/strip.gif",
                "https://cdn.discordapp.com/attachments/991784316881346601/991787556666605608/strip.gif",
                "https://cdn.discordapp.com/attachments/991784316881346601/991787670604877884/strip.gif",
                "https://cdn.discordapp.com/attachments/991784316881346601/991787737667621034/strip.gif"
        );
    }

}
