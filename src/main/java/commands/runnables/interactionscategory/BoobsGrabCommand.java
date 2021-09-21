package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "boobsgrab",
        emoji = "\uD83C\uDF52",
        executableWithoutArgs = true,
        nsfw = true,
        requiresFullMemberCache = true,
        aliases = { "grabboobs", "tittygrab", "grabtitty", "titsgrab", "grabtits" }
)
public class BoobsGrabCommand extends RolePlayAbstract {

    public BoobsGrabCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true,
                "https://cdn.discordapp.com/attachments/834493654742728764/834493853951721502/boobsgrab.gif",
                "https://cdn.discordapp.com/attachments/834493654742728764/834493865028616232/boobsgrab.gif",
                "https://cdn.discordapp.com/attachments/834493654742728764/834493902748647424/boobsgrab.gif",
                "https://cdn.discordapp.com/attachments/834493654742728764/834493914957742110/boobsgrab.gif",
                "https://cdn.discordapp.com/attachments/834493654742728764/834493927197114429/boobsgrab.gif",
                "https://cdn.discordapp.com/attachments/834493654742728764/834493941012758578/boobsgrab.gif",
                "https://cdn.discordapp.com/attachments/834493654742728764/834493958905659422/boobsgrab.gif",
                "https://cdn.discordapp.com/attachments/834493654742728764/834493971501154316/boobsgrab.gif",
                "https://cdn.discordapp.com/attachments/834493654742728764/834493985648541776/boobsgrab.gif",
                "https://cdn.discordapp.com/attachments/834493654742728764/834494003180994660/boobsgrab.gif",
                "https://cdn.discordapp.com/attachments/834493654742728764/834494014942085190/boobsgrab.gif",
                "https://cdn.discordapp.com/attachments/834493654742728764/834494026912104528/boobsgrab.gif",
                "https://cdn.discordapp.com/attachments/834493654742728764/834494039390552114/boobsgrab.gif",
                "https://cdn.discordapp.com/attachments/834493654742728764/834494053588140053/boobsgrab.gif",
                "https://cdn.discordapp.com/attachments/834493654742728764/834494066951323658/boobsgrab.gif",
                "https://cdn.discordapp.com/attachments/834493654742728764/834494080339148840/boobsgrab.gif",
                "https://cdn.discordapp.com/attachments/834493654742728764/834494093999734792/boobsgrab.gif",
                "https://cdn.discordapp.com/attachments/834493654742728764/834494112286638210/boobsgrab.gif"
        );
    }

}
