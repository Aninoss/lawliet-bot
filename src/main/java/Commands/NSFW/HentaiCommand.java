package Commands.NSFW;

import CommandListeners.CommandProperties;

@CommandProperties(
        trigger = "hentai",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        withLoadingBar = true
)
public class HentaiCommand extends GelbooruProxyCommand {
    public HentaiCommand() {
        super("animated_gif -yaoi -yuri", true);
    }
}