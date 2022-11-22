package commands.runnables.nsfwcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.PornSearchAbstract;

@CommandProperties(
        trigger = "gelb",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        aliases = { "gbooru", "gelbooru" }
)
public class GelbooruCommand extends PornSearchAbstract {

    public GelbooruCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public String getDomain() {
        return "gelbooru.com";
    }

    @Override
    public boolean isExplicit() {
        return true;
    }

}
