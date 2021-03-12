package commands.runnables.nsfwcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import commands.runnables.PornSearchAbstract;

@CommandProperties(
        trigger = "gelb",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        withLoadingBar = true,
        aliases = { "gel", "gbooru", "gelbooru" }
)
public class GelbooruCommand extends PornSearchAbstract implements OnAlertListener {

    public GelbooruCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getDomain() {
        return "gelbooru.com";
    }

    @Override
    protected String getImageTemplate() {
        return "https://simg3.gelbooru.com/samples/%d/sample_%f";
    }

    @Override
    public boolean isExplicit() {
        return true;
    }

}
