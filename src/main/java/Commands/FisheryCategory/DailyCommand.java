package Commands.FisheryCategory;

import CommandListeners.CommandProperties;
import CommandSupporters.Command;
import Commands.FisheryAbstract;
import Constants.*;
import Core.*;
import Core.Utils.BotUtil;
import Core.Utils.StringUtil;
import Core.Utils.TimeUtil;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryUserBean;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@CommandProperties(
    trigger = "daily",
    botPermissions = Permission.USE_EXTERNAL_EMOJIS,
    thumbnail = "http://icons.iconarchive.com/icons/fps.hu/free-christmas-flat-circle/128/calendar-icon.png",
    emoji = "\uD83D\uDDD3",
    executable = true
)
public class DailyCommand extends FisheryAbstract {

    @Override
    public boolean onMessageReceivedSuccessful(MessageCreateEvent event, String followedString) throws Throwable {
        FisheryUserBean userBean = DBFishery.getInstance().getBean(event.getServer().get().getId()).getUserBean(event.getMessageAuthor().getId());
        if (!userBean.getDailyReceived().equals(LocalDate.now())) {
            long fishes = userBean.getPowerUp(FisheryCategoryInterface.PER_DAY).getEffect();
            boolean breakStreak = userBean.getDailyStreak() != 0 && !userBean.getDailyReceived().plus(1, ChronoUnit.DAYS).equals(LocalDate.now());
            userBean.updateDailyReceived();

            int bonusCombo = 0;
            int bonusDonation = 0;
            int dailyStreakNow = breakStreak ? 1 : userBean.getDailyStreak() + 1;

            if (dailyStreakNow >= 5) {
                bonusCombo = (int) Math.round(fishes * 0.25);
            }

            if (PatreonCache.getInstance().getPatreonLevel(event.getMessageAuthor().getId()) > 0) {
                bonusDonation = (int) Math.round((fishes + bonusCombo) * 0.5);
            }

            StringBuilder sb = new StringBuilder(getString("point_default", StringUtil.numToString(getLocale(), fishes)));
            if (bonusCombo > 0)
                sb.append("\n").append(getString("point_combo", StringUtil.numToString(getLocale(), bonusCombo)));
            if (bonusDonation > 0)
                sb.append("\n").append(getString("point_donation", StringUtil.numToString(getLocale(), bonusDonation)));

            EmbedBuilder eb = EmbedFactory.getCommandEmbedSuccess(this, getString("codeblock", sb.toString()));
            eb.addField(getString("didyouknow_title"), getString("didyouknow_desc", Settings.PATREON_PAGE, Settings.UPVOTE_URL), false);
            if (breakStreak) EmbedFactory.addLog(eb, LogStatus.LOSE, getString("combobreak"));

            event.getChannel().sendMessage(eb).get();
            event.getChannel().sendMessage(userBean.changeValues(fishes + bonusCombo + bonusDonation, 0, dailyStreakNow)).get();

            return true;
        } else {
            Instant nextDaily = TimeUtil.setInstantToNextDay(Instant.now());

            EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this, getString("claimed_desription"), getString("claimed_title"));
            EmbedFactory.addLog(eb, null, TextManager.getString(getLocale(), TextManager.GENERAL, "next", TimeUtil.getRemainingTimeString(getLocale(), Instant.now(), nextDaily, false)));
            event.getChannel().sendMessage(eb).get();
            return false;
        }
    }
}
