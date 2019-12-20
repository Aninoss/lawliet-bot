package Commands.NSFW;

import CommandListeners.CommandProperties;

@CommandProperties(
        trigger = "rlporn",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        withLoadingBar = true
)
public class RealLifePornCommand extends RealbooruProxyCommand {
    public RealLifePornCommand() {
        super("animated -gay -lesbian -trap -shemale", true);
    }
}