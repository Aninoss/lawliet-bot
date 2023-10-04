package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.RunPodAbstract;
import constants.Settings;

import java.util.Locale;

@CommandProperties(
        trigger = "txt2hentai",
        emoji = "🖌️",
        executableWithoutArgs = false,
        patreonRequired = true,
        nsfw = true,
        aliases = {"nsfwimagine", "imaginensfw"}
)
public class Txt2HentaiCommand extends RunPodAbstract {

    public Txt2HentaiCommand(Locale locale, String prefix) {
        super(locale, prefix, Settings.NSFW_FILTERS);
    }

}
