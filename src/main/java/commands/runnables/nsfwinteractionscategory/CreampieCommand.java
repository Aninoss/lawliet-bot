package commands.runnables.nsfwinteractionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "creampie",
        emoji = "🥧",
        executableWithoutArgs = true,
        nsfw = true,
        requiresFullMemberCache = true,
        aliases = { "impregnate" }
)
public class CreampieCommand extends RolePlayAbstract {

    public CreampieCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false);

        setFtfGifs(
                "https://cdn.discordapp.com/attachments/955839621902774272/955839871052808292/creampie.gif"
        );
        setMtfGifs(
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
                "https://cdn.discordapp.com/attachments/955839621902774272/955842659140898816/creampie.gif",
                "https://cdn.discordapp.com/attachments/955839621902774272/1073601527287119964/creampie.gif",
                "https://cdn.discordapp.com/attachments/955839621902774272/1073602819321507840/creampie.gif",
                "https://cdn.discordapp.com/attachments/955839621902774272/1073602940889206824/creampie.gif",
                "https://cdn.discordapp.com/attachments/955839621902774272/1073603217922994237/creampie.gif",
                "https://cdn.discordapp.com/attachments/955839621902774272/1073603841506955345/creampie.gif",
                "https://cdn.discordapp.com/attachments/955839621902774272/1073604277651648532/creampie.gif",
                "https://cdn.discordapp.com/attachments/955839621902774272/1130880259626111097/creampie.gif",
                "https://cdn.discordapp.com/attachments/955839621902774272/1151398338701299783/creampie.gif"
        );
        setAtfGifs(
        );
        setAtaGifs(
                "https://cdn.discordapp.com/attachments/955839621902774272/1073604357951606784/creampie.gif"
        );
    }

}
