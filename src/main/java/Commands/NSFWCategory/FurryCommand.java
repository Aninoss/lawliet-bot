package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import Commands.PornSearchAbstract;

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
