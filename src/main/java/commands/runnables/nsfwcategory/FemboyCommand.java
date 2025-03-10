package commands.runnables.nsfwcategory;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import commands.listeners.CommandProperties;
import commands.runnables.RealbooruAbstract;

@CommandProperties(
        trigger = "femboy",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        patreonRequired = true,
        aliases = { "rlfemboy", "reallifefemboy" }
)
public class FemboyCommand extends RealbooruAbstract {

    public FemboyCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "femboy";
    }

    @Override
    protected Set<String> getAdditionalFilters() {
        return Collections.emptySet();
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}