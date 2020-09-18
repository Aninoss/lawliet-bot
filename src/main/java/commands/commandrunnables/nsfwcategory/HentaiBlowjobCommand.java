package commands.commandrunnables.nsfwcategory;

import commands.commandlisteners.CommandProperties;
import commands.commandrunnables.GelbooruAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "hblowjob",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        patreonRequired = true,
        withLoadingBar = true,
        aliases = { "hentaiblowjob", "hbj" }
)
public class HentaiBlowjobCommand extends GelbooruAbstract {

    public HentaiBlowjobCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "animated blowjob -gay -lesbian -trap -shemale";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}