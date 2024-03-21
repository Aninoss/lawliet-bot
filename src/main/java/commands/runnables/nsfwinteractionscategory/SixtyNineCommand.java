package commands.runnables.nsfwinteractionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "69",
        emoji = "👅",
        executableWithoutArgs = true,
        nsfw = true,
        requiresFullMemberCache = true,
        aliases = "sixtynine"
)
public class SixtyNineCommand extends RolePlayAbstract {

    public SixtyNineCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true,
                "https://cdn.discordapp.com/attachments/969973669126340758/969973746679046154/69.gif",
                "https://cdn.discordapp.com/attachments/969973669126340758/969973986052153364/69.gif",
                "https://cdn.discordapp.com/attachments/969973669126340758/969974210329997363/69.gif",
                "https://cdn.discordapp.com/attachments/969973669126340758/969974239987892304/69.gif",
                "https://cdn.discordapp.com/attachments/969973669126340758/969974274003714068/69.gif",
                "https://cdn.discordapp.com/attachments/969973669126340758/969974301258293268/69.gif",
                "https://cdn.discordapp.com/attachments/969973669126340758/969974337576796180/69.gif",
                "https://cdn.discordapp.com/attachments/969973669126340758/969974368648175706/69.gif",
                "https://cdn.discordapp.com/attachments/969973669126340758/969974394178928660/69.gif",
                "https://cdn.discordapp.com/attachments/969973669126340758/969974427569762314/69.gif",
                "https://cdn.discordapp.com/attachments/969973669126340758/1220286428055867443/69.gif"
        );
    }

}
