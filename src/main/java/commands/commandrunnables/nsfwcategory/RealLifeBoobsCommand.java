package commands.commandrunnables.nsfwcategory;

import commands.commandlisteners.CommandProperties;
import commands.commandrunnables.RealbooruAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "rlboobs",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        patreonRequired = true,
        withLoadingBar = true,
        aliases = {"boobs"}
)
public class RealLifeBoobsCommand extends RealbooruAbstract {

    public RealLifeBoobsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "boobs -gay -lesbian -trap -shemale";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

}