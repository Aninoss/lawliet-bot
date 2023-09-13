package commands.runnables.nsfwinteractionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "dickride",
        emoji = "🍆",
        executableWithoutArgs = true,
        nsfw = true,
        requiresFullMemberCache = true,
        aliases = "ridedick"
)
public class DickRideCommand extends RolePlayAbstract {

    public DickRideCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true,
                "https://cdn.discordapp.com/attachments/969967119469019136/969967204336549938/dickride.gif",
                "https://cdn.discordapp.com/attachments/969967119469019136/969967744403517520/dickride.gif",
                "https://cdn.discordapp.com/attachments/969967119469019136/969967825382940702/dickride.gif",
                "https://cdn.discordapp.com/attachments/969967119469019136/969967859918856222/dickride.gif",
                "https://cdn.discordapp.com/attachments/969967119469019136/969967894752559114/dickride.gif",
                "https://cdn.discordapp.com/attachments/969967119469019136/969967934040596500/dickride.gif",
                "https://cdn.discordapp.com/attachments/969967119469019136/969967969511833630/dickride.gif",
                "https://cdn.discordapp.com/attachments/969967119469019136/969967999471743066/dickride.gif",
                "https://cdn.discordapp.com/attachments/969967119469019136/969968026818580530/dickride.gif",
                "https://cdn.discordapp.com/attachments/969967119469019136/969968480147357839/dickride.gif",
                "https://cdn.discordapp.com/attachments/969967119469019136/969968872272830474/dickride.gif",
                "https://cdn.discordapp.com/attachments/969967119469019136/1151398675667501086/dickride.gif",
                "https://cdn.discordapp.com/attachments/969967119469019136/1151398874825637908/dickride.gif"
        );
    }

}
