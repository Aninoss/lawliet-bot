package commands.runnables.nsfwcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.Rule34HentaiAbstract;

@CommandProperties(
        trigger = "hblowjob",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        patreonRequired = true,
        aliases = { "hentaiblowjob", "hbj" }
)
public class HentaiBlowjobCommand extends Rule34HentaiAbstract {

    public HentaiBlowjobCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "animated fellatio -yaoi -yuri -shemale -lesbian -gay -futa -trap";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}