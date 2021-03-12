package commands.runnables.nsfwcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.GelbooruAbstract;

@CommandProperties(
        trigger = "hblowjob",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        patreonRequired = true,
        aliases = { "hentaiblowjob", "hbj" }
)
public class HentaiBlowjobCommand extends GelbooruAbstract {

    public HentaiBlowjobCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "animated blowjob -gay -lesbian -trap -shemale -interspecies -furry";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}