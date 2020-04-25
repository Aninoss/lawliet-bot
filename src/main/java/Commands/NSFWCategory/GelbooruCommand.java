package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import Commands.PornSearchAbstract;

@CommandProperties(
        trigger = "gelb",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        requiresEmbeds = false,
        withLoadingBar = true,
        aliases = {"gel", "gbooru", "gelbooru"}
)
public class GelbooruCommand extends PornSearchAbstract {

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
