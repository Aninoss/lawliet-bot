package Commands.NSFW;

import CommandListeners.CommandProperties;

@CommandProperties(
        trigger = "trap",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        withLoadingBar = true
)
public class TrapCommand extends GelbooruProxyCommand {
    public TrapCommand() {
        super("animated_gif trap", true);
    }
}