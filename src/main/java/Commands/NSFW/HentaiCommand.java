package Commands.NSFW;

public class HentaiCommand extends GelbooruProxyCommand {
    public HentaiCommand() {
        super("animated_gif -yaoi -yuri", true);
        trigger = "hentai";
    }
}