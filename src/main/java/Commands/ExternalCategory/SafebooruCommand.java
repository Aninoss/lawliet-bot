package Commands.ExternalCategory;

import CommandListeners.CommandProperties;
import Commands.PornSearchAbstract;

@CommandProperties(
        trigger = "safeb",
        emoji = "\uD83D\uDE07",
        withLoadingBar = true,
        executable = true,
        aliases = {"softb", "safebooru", "softbooru", "sbooru"}
)
public class SafebooruCommand extends PornSearchAbstract {

    @Override
    protected String getDomain() {
        return "safebooru.org";
    }

    @Override
    protected String getImageTemplate() {
        return "https://safebooru.org/images/%d/%f";
    }

}
