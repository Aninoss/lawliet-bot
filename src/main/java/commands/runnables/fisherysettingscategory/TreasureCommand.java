package commands.runnables.fisherysettingscategory;

import commands.listeners.CommandProperties;
import commands.runnables.FisheryAbstract;
import constants.PermissionDeprecated;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import modules.Fishery;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Locale;

@CommandProperties(
        trigger = "treasure",
        userPermissions = PermissionDeprecated.MANAGE_SERVER,
        emoji = "\uD83C\uDFF4\u200D☠️",
        executableWithoutArgs = true,
        patreonRequired = true,
        aliases = { "tresure", "treasurechest" }
)
public class TreasureCommand extends FisheryAbstract {

    public TreasureCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceivedSuccessful(MessageCreateEvent event, String followedString) throws Throwable {
        int amount = 1;
        if (followedString.length() > 0) {
            if (StringUtil.stringIsInt(followedString)) {
                amount = Integer.parseInt(followedString);
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

        for(int i = 0; i < amount; i++) Fishery.spawnTreasureChest(event.getServer().get().getId(), event.getServerTextChannel().get());
        return true;
    }

}
