package Commands.NSFW;

import CommandListeners.CommandProperties;

@CommandProperties(
        trigger = "yuri",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        withLoadingBar = true
)
public class YuriCommand extends GelbooruProxyCommand {
    public YuriCommand() {
        super("animated_gif yuri", true);
    }
}