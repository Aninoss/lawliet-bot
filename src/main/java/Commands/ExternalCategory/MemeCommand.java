package Commands.ExternalCategory;

import CommandListeners.CommandProperties;
import Commands.RedditAbstract;

@CommandProperties(
        trigger = "meme",
        nsfw = false,
        emoji = "\uD83D\uDDBC",
        withLoadingBar = true,
        executable = true
)
public class MemeCommand extends RedditAbstract {

    @Override
    public String getSubreddit() { return "memes"; }

}
