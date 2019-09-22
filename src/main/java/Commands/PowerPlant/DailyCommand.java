package Commands.PowerPlant;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.FishingCategoryInterface;
import Constants.Permission;
import Constants.PowerPlantStatus;
import Constants.Settings;
import General.*;
import General.Fishing.FishingProfile;
import MySQL.DBServer;
import MySQL.DBUser;
import MySQL.DailyState;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

@CommandProperties(
    trigger = "daily",
    botPermissions = Permission.USE_EXTERNAL_EMOJIS_IN_TEXT_CHANNEL,
    thumbnail = "http://icons.iconarchive.com/icons/fps.hu/free-christmas-flat-circle/128/calendar-icon.png",
    emoji = "\uD83D\uDDD3",
    executable = true
)
public class DailyCommand extends Command implements onRecievedListener {

    public DailyCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        PowerPlantStatus status = DBServer.getPowerPlantStatusFromServer(event.getServer().get());
        if (status == PowerPlantStatus.ACTIVE) {
            DailyState dailyState = DBUser.daily(event.getServer().get(), event.getMessage().getUserAuthor().get());
            if (dailyState.isClaimed()) {
                FishingProfile fishingProfile = DBUser.getFishingProfile(event.getServer().get(), event.getMessage().getUserAuthor().get());

                long fishes = fishingProfile.getEffect(FishingCategoryInterface.PER_DAY);

                int bonusCombo = 0;
                int bonusDonation = 0;
                int dailyBefore = -1;
                String label;
                if (dailyState.isStreakBroken()) {
                    label = "successful_combobreak";
                    dailyBefore = dailyState.getStreak();
                } else {
                    dailyBefore = dailyState.getStreak()-1;
                    if (dailyState.getStreak() < 5) {
                        label = "successful_nocombo";
                    } else {
                        label = "successful_highcombo";
                        bonusCombo = (int) Math.round(fishes * 0.25);
                    }
                }

                if (DBUser.hasDonated(event.getMessage().getUserAuthor().get())) {
                    bonusDonation = (int) Math.round((fishes + bonusCombo) * 0.5);
                }

                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedSuccess(this, getString(label, dailyState.getStreak() != 1, Tools.numToString(getLocale(), fishes), Tools.numToString(getLocale(), dailyState.getStreak()), Tools.numToString(getLocale(), bonusCombo))));
                if (bonusDonation > 0) event.getChannel().sendMessage(EmbedFactory.getCommandEmbedSuccess(this, getString("donate_description", Tools.numToString(getLocale(), bonusDonation), Settings.UPVOTE_URL), getString("donate_title")).setThumbnail("http://icons.iconarchive.com/icons/graphicloads/flat-finance/128/dollar-icon.png"));
                else event.getChannel().sendMessage(EmbedFactory.getCommandEmbedSuccess(this, getString("upvote_description", Settings.UPVOTE_URL), getString("upvote_title")));
                event.getChannel().sendMessage(DBUser.addFishingValues(getLocale(), event.getServer().get(), event.getMessage().getUserAuthor().get(), fishes + bonusCombo + bonusDonation, 0L, dailyBefore)).get();
                return true;
            } else {
                EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this, getString("claimed_desription"), getString("claimed_title"));
                event.getChannel().sendMessage(eb).get();
                return false;
            }
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title")));
            return false;
        }
    }
}
