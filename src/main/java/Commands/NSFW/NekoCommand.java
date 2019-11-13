package Commands.NSFW;

import CommandListeners.CommandProperties;

@CommandProperties(
        trigger = "neko",
        executable = true,
        emoji = "\uD83D\uDC3E",
        nsfw = true,
        withLoadingBar = true
)
public class NekoCommand extends Rule34ProxyCommand {
    public NekoCommand() {
        super("animated_gif cat_girl", true);
    }
}