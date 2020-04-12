package Commands.FisheryCategory;

import CommandListeners.CommandProperties;
import CommandSupporters.Command;
import Constants.*;
import Core.*;
import Core.Tools.StringTools;
import MySQL.Modules.Donators.DBDonators;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryUserBean;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@CommandProperties(
    trigger = "daily",
    botPermissions = Permission.USE_EXTERNAL_EMOJIS,
    thumbnail = "http://icons.iconarchive.com/icons/fps.hu/free-christmas-flat-circle/128/calendar-icon.png",
    emoji = "\uD83D\uDDD3",
    executable = true
)
public class DailyCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        FisheryStatus status = DBServer.getInstance().getBean(event.getServer().get().getId()).getFisheryStatus();
        if (status == FisheryStatus.ACTIVE) {
            FisheryUserBean userBean = DBFishery.getInstance().getBean(event.getServer().get().getId()).getUser(event.getMessageAuthor().getId());
            if (!userBean.getDailyReceived().equals(LocalDate.now())) {
                long fishes = userBean.getPowerUp(FishingCategoryInterface.PER_DAY).getEffect();
                boolean breakStreak = userBean.getDailyStreak() != 0 && !userBean.getDailyReceived().plus(1, ChronoUnit.DAYS).equals(LocalDate.now());
                userBean.updateDailyReceived();

                int bonusCombo = 0;
                int bonusDonation = 0;
                int dailyStreakNow = breakStreak ? 1 : userBean.getDailyStreak() + 1;

                if (dailyStreakNow >= 5) {
                    bonusCombo = (int) Math.round(fishes * 0.25);
                }

                if (DBDonators.getInstance().getBean().getMap().containsKey(event.getMessage().getUserAuthor().get().getId())) {
                    bonusDonation = (int) Math.round((fishes + bonusCombo) * 0.5);
                }

                StringBuilder sb = new StringBuilder(getString("point_default", StringTools.numToString(getLocale(), fishes)));
                if (bonusCombo > 0) sb.append("\n").append(getString("point_combo", StringTools.numToString(getLocale(), bonusCombo)));
                if (bonusDonation > 0) sb.append("\n").append(getString("point_donation", StringTools.numToString(getLocale(), bonusDonation)));

                EmbedBuilder eb = EmbedFactory.getCommandEmbedSuccess(this, getString("codeblock", sb.toString()));
                eb.addField(getString("didyouknow_title"), getString("didyouknow_desc", Settings.UPVOTE_URL), false);
                if (breakStreak) EmbedFactory.addLog(eb, LogStatus.LOSE, getString("combobreak"));

                event.getChannel().sendMessage(eb).get();
                event.getChannel().sendMessage(userBean.changeValues(fishes + bonusCombo + bonusDonation, 0, dailyStreakNow)).get();

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
