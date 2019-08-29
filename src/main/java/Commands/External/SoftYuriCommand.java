package Commands.External;

public class SoftYuriCommand extends SafebooruProxyCommand {
    public SoftYuriCommand() {
        super("yuri", false);
        trigger = "softyuri";
        nsfw = false;
        emoji = "\uD83D\uDC6D";
    }
}