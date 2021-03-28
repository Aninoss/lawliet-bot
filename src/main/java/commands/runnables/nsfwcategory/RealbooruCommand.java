package commands.runnables.nsfwcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.PornSearchAbstract;

@CommandProperties(
        trigger = "realb",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        aliases = { "rbooru", "realbooru", "rlbooru" }
)
public class RealbooruCommand extends PornSearchAbstract {

    public RealbooruCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getDomain() {
        return "realbooru.com";
    }

    @Override
    protected String getImageTemplate() {
        return "https://realbooru.com/images/%d/%f";
    }

    @Override
    public boolean isExplicit() {
        return true;
    }

}
