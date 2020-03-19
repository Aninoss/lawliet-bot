package Commands.ExternalCategory;

import CommandListeners.CommandProperties;
import Commands.RedditAbstract;

@CommandProperties(
    trigger = "wholesome",
    emoji = "\uD83D\uDC96",
    executable = true
)
public class WholesomeCommand extends RedditAbstract {

    @Override
    public String getSubreddit() { return "wholesomememes"; }

}
