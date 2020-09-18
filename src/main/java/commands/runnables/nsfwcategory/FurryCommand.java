package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.PornSearchAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "furry",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        withLoadingBar = true,
        aliases = { "furrybooru", "yiff" }
)
public class FurryCommand extends PornSearchAbstract {

    public FurryCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getDomain() {
        return "furry.booru.org";
    }

    @Override
    protected String getImageTemplate() {
        return "https://furry.booru.org/images/%d/%f";
    }

    @Override
    public boolean isExplicit() { return true; }

}
