package Commands.NSFW;

import CommandListeners.CommandProperties;

@CommandProperties(
        trigger = "yaoi",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        withLoadingBar = true
)
public class YaoiCommand extends GelbooruProxyCommand {
    public YaoiCommand() {
        super("animated_gif yaoi", true);
    }
}