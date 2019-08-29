package Commands.External;

import Commands.NSFW.GelbooruProxyCommand;

public class SoftYaoiCommand extends SafebooruProxyCommand {
    public SoftYaoiCommand() {
        super("yaoi", false);
        trigger = "softyaoi";
        nsfw = false;
        emoji = "\uD83D\uDC6C";
    }
}