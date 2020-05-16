package Commands.ExternalCategory;

import CommandListeners.CommandProperties;
import Commands.RedditAbstract;

@CommandProperties(
        trigger = "meme",
        emoji = "\uD83D\uDDBC",
        withLoadingBar = true,
        executable = true
)
public class MemeCommand extends RedditAbstract {

    @Override
    public String getSubreddit() { return "memes"; }

}
