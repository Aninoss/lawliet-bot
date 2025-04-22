package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "drink",
        emoji = "\uD83C\uDF7A",
        executableWithoutArgs = true,
        aliases = { "beer", "alcohol", "chug", "drunk", "cheers" }
)
public class DrinkCommand extends RolePlayAbstract {

    public DrinkCommand(Locale locale, String prefix) {
        super(locale, prefix, false);

        setFtaGifs(
                "https://cdn.discordapp.com/attachments/860580103284195358/860580167989329930/drink.gif",
                "https://cdn.discordapp.com/attachments/860580103284195358/860583818494607390/drink.gif",
                "https://cdn.discordapp.com/attachments/860580103284195358/860584584902213642/drink.gif",
                "https://cdn.discordapp.com/attachments/860580103284195358/860584802670477323/drink.gif",
                "https://cdn.discordapp.com/attachments/860580103284195358/860585022653071375/drink.gif"
        );
        setMtaGifs(
                "https://cdn.discordapp.com/attachments/860580103284195358/860583666723586058/drink.gif",
                "https://cdn.discordapp.com/attachments/860580103284195358/860584917146796032/drink.gif"
        );
        setAtaGifs(
                "https://cdn.discordapp.com/attachments/860580103284195358/860583931501084682/drink.gif",
                "https://cdn.discordapp.com/attachments/860580103284195358/860584699037745152/drink.gif"
        );
    }

}
