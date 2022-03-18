package commands.runnables.nsfwcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.PornSearchAbstract;

@CommandProperties(
        trigger = "e621",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        aliases = { "furryb", "furrybooru", "furrb" }
)
public class E621Command extends PornSearchAbstract {

    public E621Command(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public String getDomain() {
        return "e621.net";
    }

    @Override
    public boolean isExplicit() {
        return true;
    }

    @Override
    public int getMaxTags() {
        return 40;
    }

}
