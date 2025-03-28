package commands.runnables.nsfwcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RealbooruAbstract;

@CommandProperties(
        trigger = "rllesbian",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        aliases = { "lesbian", "reallifelesbian", "lesbianporn", "rllesbianporn", "reallifelesbianporn" }
)
public class RealLifeLesbianCommand extends RealbooruAbstract {

    public RealLifeLesbianCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getSearchKey() {
        return "lesbian";
    }

    @Override
    protected boolean isAnimatedOnly() {
        return true;
    }

}