package Commands.NSFW;

import CommandListeners.CommandProperties;

@CommandProperties(
        trigger = "futa",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        withLoadingBar = true
)
public class FutaCommand extends GelbooruProxyCommand {
    public FutaCommand() {
        super("animated_gif futa", true);
    }
}