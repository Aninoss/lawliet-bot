package Commands.FisherySettingsCategory;

import CommandListeners.CommandProperties;
import Commands.FisheryAbstract;
import Constants.Permission;
import Core.EmbedFactory;
import Core.TextManager;
import Core.Utils.StringUtil;
import Modules.Fishery;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Locale;

@CommandProperties(
        trigger = "treasure",
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "\uD83C\uDFF4\u200D☠️",
        executable = true,
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
                    event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "30"))).get();
                    return false;
                }
            } else {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                        TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"))).get();
                return false;
            }
        }

        for(int i = 0; i < amount; i++) Fishery.spawnTreasureChest(event.getServer().get().getId(), event.getServerTextChannel().get());
        return true;
    }

}
