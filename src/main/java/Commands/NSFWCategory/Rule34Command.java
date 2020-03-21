package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import Commands.PornSearchAbstract;

@CommandProperties(
        trigger = "r34",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        withLoadingBar = true,
        aliases = {"rule34", "34"}
)
public class Rule34Command extends PornSearchAbstract {

    @Override
    protected String getDomain() {
        return "rule34.xxx";
    }

    @Override
    protected String getImageTemplate() {
        return "https://img.rule34.xxx/images/%d/%f";
    }

}
