package Commands.FisheryCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.FishingCategoryInterface;
import Constants.Permission;
import Constants.PowerPlantStatus;
import Constants.Settings;
import General.EmbedFactory;
import General.TextManager;
import General.Tools;
import MySQL.DBServer;
import MySQL.DBUser;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

@CommandProperties(
    trigger = "claim",
    botPermissions = Permission.USE_EXTERNAL_EMOJIS_IN_TEXT_CHANNEL,
    thumbnail = "http://icons.iconarchive.com/icons/fps.hu/free-christmas-flat-circle/128/gift-icon.png",
    emoji = "\uD83C\uDF80",
    executable = true
)
public class ClaimCommand extends Command implements onRecievedListener {

    public ClaimCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        PowerPlantStatus status = DBServer.getPowerPlantStatusFromServer(event.getServer().get());
        if (status == PowerPlantStatus.ACTIVE) {
            int upvotesUnclaimed = DBUser.getUpvotesUnclaimed(event.getServer().get(), event.getMessage().getUserAuthor().get());

            if (upvotesUnclaimed == 0) {
                EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this, getString("nothing_description", Settings.UPVOTE_URL), getString("nothing_title"));
                event.getChannel().sendMessage(eb).get();
                return false;
            } else {
                long fishes = DBUser.getFishingProfile(event.getServer().get(), event.getMessage().getUserAuthor().get()).getEffect(FishingCategoryInterface.PER_DAY);

                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedSuccess(this, getString("claim", upvotesUnclaimed != 1, Tools.numToString(getLocale(), upvotesUnclaimed), Tools.numToString(getLocale(), Math.round(fishes * 0.25 * upvotesUnclaimed)), Settings.UPVOTE_URL)));
                event.getChannel().sendMessage(DBUser.addFishingValues(getLocale(), event.getServer().get(), event.getMessage().getUserAuthor().get(), Math.round(fishes * 0.25 * upvotesUnclaimed), 0L)).get();
                return true;
            }
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title")));
            return false;
        }
    }
}
