package Commands.External;

import CommandListeners.CommandProperties;
import Commands.NSFW.RedditTemplateCommand;

@CommandProperties(
    trigger = "wholesome",
    emoji = "\uD83D\uDC96",
    executable = true
)
public class WholesomeCommand extends RedditTemplateCommand {

    public WholesomeCommand() {
        super("wholesomememes");
    }

}
