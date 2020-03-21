package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import Commands.PornSearchAbstract;

@CommandProperties(
        trigger = "realb",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        withLoadingBar = true,
        aliases = {"rbooru", "realbooru"}
)
public class RealbooruCommand extends PornSearchAbstract {

    @Override
    protected String getDomain() {
        return "realbooru.com";
    }

    @Override
    protected String getImageTemplate() {
        return "https://realbooru.com/images/%d/%f";
    }

}
