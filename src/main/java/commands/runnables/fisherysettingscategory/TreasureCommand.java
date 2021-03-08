package commands.runnables.fisherysettingscategory;

import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import modules.Fishery;

@CommandProperties(
        trigger = "treasure",
        userPermissions = PermissionDeprecated.MANAGE_SERVER,
        emoji = "\uD83C\uDFF4\u200D☠️",
        executableWithoutArgs = true,
        patreonRequired = true,
        aliases = { "tresure", "treasurechest" }
)
public class TreasureCommand extends Command implements FisheryInterface {

    public TreasureCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceivedSuccessful(MessageCreateEvent event, String args) throws Throwable {
        int amount = 1;
        if (args.length() > 0) {
            if (StringUtil.stringIsInt(args)) {
                amount = Integer.parseInt(args);
                if (amount < 1 || amount > 30) {
                    event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "30"))).get();
                    return false;
                }
            } else {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                        TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"))).get();
                return false;
            }
        }

        for(int i = 0; i < amount; i++) Fishery.spawnTreasureChest(event.getGuild().getIdLong(), event.getServerTextChannel().get());
        return true;
    }

}
