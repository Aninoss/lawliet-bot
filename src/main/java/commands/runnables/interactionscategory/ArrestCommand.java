package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "arrest",
        emoji = "👮",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = {"handcuffs"}
)
public class ArrestCommand extends RolePlayAbstract {

    public ArrestCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false);

        setFtfGifs(
                "https://cdn.discordapp.com/attachments/992095513073700994/992095593394622475/arrest.gif"
        );
        setFtaGifs(
                "https://cdn.discordapp.com/attachments/992095513073700994/992096182409109675/arrest.gif"
        );
        setAtfGifs(
                "https://cdn.discordapp.com/attachments/992095513073700994/992095783438520361/arrest.gif",
                "https://cdn.discordapp.com/attachments/992095513073700994/992095956130607295/arrest.gif",
                "https://cdn.discordapp.com/attachments/992095513073700994/992096151895556217/arrest.gif",
                "https://cdn.discordapp.com/attachments/992095513073700994/992096227699216425/arrest.gif"
        );
        setAtmGifs(
                "https://cdn.discordapp.com/attachments/992095513073700994/992095641901744198/arrest.gif"
        );
        setAtaGifs(
                "https://cdn.discordapp.com/attachments/992095513073700994/992095681474986025/arrest.gif",
                "https://cdn.discordapp.com/attachments/992095513073700994/992095919736619128/arrest.gif",
                "https://cdn.discordapp.com/attachments/992095513073700994/992096116868915321/arrest.gif"
        );
    }

}
