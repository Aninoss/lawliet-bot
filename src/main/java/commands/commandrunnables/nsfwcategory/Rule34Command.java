package commands.commandrunnables.nsfwcategory;

import commands.commandlisteners.CommandProperties;
import commands.commandlisteners.OnTrackerRequestListener;
import commands.commandrunnables.PornSearchAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "r34",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        withLoadingBar = true,
        aliases = {"rule34", "34"}
)
public class Rule34Command extends PornSearchAbstract implements OnTrackerRequestListener {

    public Rule34Command(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getDomain() {
        return "rule34.xxx";
    }

    @Override
    protected String getImageTemplate() {
        return "https://img.rule34.xxx/images/%d/%f";
    }

    @Override
    public boolean isExplicit() { return true; }

}
