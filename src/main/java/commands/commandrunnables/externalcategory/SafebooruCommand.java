package commands.commandrunnables.externalcategory;

import commands.commandlisteners.CommandProperties;
import commands.commandrunnables.PornSearchAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "safeb",
        emoji = "\uD83D\uDE07",
        withLoadingBar = true,
        executable = true,
        aliases = {"softb", "safebooru", "softbooru", "sbooru"}
)
public class SafebooruCommand extends PornSearchAbstract {

    public SafebooruCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getDomain() {
        return "safebooru.org";
    }

    @Override
    protected String getImageTemplate() {
        return "https://safebooru.org/images/%d/%f";
    }

    @Override
    public boolean isExplicit() { return false; }

}
