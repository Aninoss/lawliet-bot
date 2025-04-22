package commands.runnables.nsfwinteractionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "facesit",
        emoji = "🪑",
        executableWithoutArgs = true,
        nsfw = true,
        requiresFullMemberCache = true,
        aliases = { "sitface" }
)
public class FaceSitCommand extends RolePlayAbstract {

    public FaceSitCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false);

        setFtfGifs(
                "https://cdn.discordapp.com/attachments/969970754944909332/1220286016531992606/facesit.gif"
        );
        setFtmGifs(
                "https://cdn.discordapp.com/attachments/969970754944909332/969971027041980457/facesit.gif",
                "https://cdn.discordapp.com/attachments/969970754944909332/969971063696031764/facesit.gif",
                "https://cdn.discordapp.com/attachments/969970754944909332/969971096717766666/facesit.gif",
                "https://cdn.discordapp.com/attachments/969970754944909332/969971128468668467/facesit.gif",
                "https://cdn.discordapp.com/attachments/969970754944909332/969971163793063986/facesit.gif",
                "https://cdn.discordapp.com/attachments/969970754944909332/969971197976657970/facesit.gif",
                "https://cdn.discordapp.com/attachments/969970754944909332/969971565989093406/facesit.gif",
                "https://cdn.discordapp.com/attachments/969970754944909332/969971921791893504/facesit.gif",
                "https://cdn.discordapp.com/attachments/969970754944909332/969971959108620319/facesit.gif",
                "https://cdn.discordapp.com/attachments/969970754944909332/969971995544526868/facesit.gif",
                "https://cdn.discordapp.com/attachments/969970754944909332/969972020102185020/facesit.gif",
                "https://cdn.discordapp.com/attachments/969970754944909332/1130881488284561530/facesit.gif",
                "https://cdn.discordapp.com/attachments/969970754944909332/1130882325698314361/facesit.gif"
        );
    }

}
