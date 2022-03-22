package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "creampie",
        emoji = "🥧",
        executableWithoutArgs = true,
        nsfw = true,
        requiresFullMemberCache = true
)
public class CreampieCommand extends RolePlayAbstract {

    public CreampieCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true,
                "https://cdn.discordapp.com/attachments/955839621902774272/955839871052808292/creampie.gif",
                "https://cdn.discordapp.com/attachments/955839621902774272/955839955505139752/creampie.gif",
                "https://cdn.discordapp.com/attachments/955839621902774272/955840066230554644/creampie.gif",
                "https://cdn.discordapp.com/attachments/955839621902774272/955840111520649216/creampie.gif",
                "https://cdn.discordapp.com/attachments/955839621902774272/955840159235063818/creampie.gif",
                "https://cdn.discordapp.com/attachments/955839621902774272/955840213391912960/creampie.gif",
                "https://cdn.discordapp.com/attachments/955839621902774272/955840275845115924/creampie.gif",
                "https://cdn.discordapp.com/attachments/955839621902774272/955840306388029460/creampie.gif",
                "https://cdn.discordapp.com/attachments/955839621902774272/955840998074896384/creampie.gif",
                "https://cdn.discordapp.com/attachments/955839621902774272/955841342288842812/creampie.gif",
                "https://cdn.discordapp.com/attachments/955839621902774272/955842304869027890/creampie.gif",
                "https://cdn.discordapp.com/attachments/955839621902774272/955842659140898816/creampie.gif"
        );
    }

}
