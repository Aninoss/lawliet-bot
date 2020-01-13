package Commands.External;

import CommandListeners.CommandProperties;

@CommandProperties(
        trigger = "meme",
        nsfw = false,
        emoji = "\uD83D\uDDBC",
        withLoadingBar = true,
        executable = true
)
public class MemeCommand extends RedditTemplateCommand {

    public MemeCommand() {
        super("memes");
    }

}
