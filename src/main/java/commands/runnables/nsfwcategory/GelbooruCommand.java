package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnTrackerRequestListener;
import commands.runnables.PornSearchAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "gelb",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        withLoadingBar = true,
        aliases = {"gel", "gbooru", "gelbooru"}
)
public class GelbooruCommand extends PornSearchAbstract implements OnTrackerRequestListener {

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
    public boolean isExplicit() { return true; }

}
