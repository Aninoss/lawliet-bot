package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.PornSearchAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "realb",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        withLoadingBar = true,
        aliases = {"rbooru", "realbooru"}
)
public class RealbooruCommand extends PornSearchAbstract {

    public RealbooruCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getDomain() {
        return "realbooru.com";
    }

    @Override
    protected String getImageTemplate() {
        return "https://realbooru.com/images/%d/%f";
    }

    @Override
    public boolean isExplicit() { return true; }

}
