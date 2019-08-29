package Commands.External;

import Commands.NSFW.RedditTemplateCommand;

public class WholesomeCommand extends RedditTemplateCommand {
    public WholesomeCommand() {
        super("wholesomememes");
        trigger = "wholesome";
        nsfw = false;
        emoji = "\uD83D\uDC96";
    }
}
