package commands.runnables.nsfwcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.PornSearchAbstract;

@CommandProperties(
        trigger = "danb",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        aliases = { "dbooru", "danbooru" }
)
public class DanbooruCommand extends PornSearchAbstract {

    public DanbooruCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getDomain() {
        return "danbooru.donmai.us";
    }

    @Override
    public boolean isExplicit() {
        return true;
    }

    @Override
    public int getMaxTags() {
        return 10;
    }

}
