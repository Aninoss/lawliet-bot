package commands.runnables.fisherycategory;

import commands.listeners.CommandProperties;
import commands.runnables.FisheryAbstract;
import constants.*;
import core.EmbedFactory;
import core.PatreonCache;
import core.TextManager;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryUserBean;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@CommandProperties(
    trigger = "daily",
    botPermissions = Permission.USE_EXTERNAL_EMOJIS,
    emoji = "\uD83D\uDDD3",
    executableWithoutArgs = true
)
public class DailyCommand extends FisheryAbstract {

    public DailyCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceivedSuccessful(MessageCreateEvent event, String followedString) throws Throwable {
        FisheryUserBean userBean = DBFishery.getInstance().getBean(event.getServer().get().getId()).getUserBean(event.getMessageAuthor().getId());
        if (!userBean.getDailyReceived().equals(LocalDate.now())) {
            long fishes = userBean.getPowerUp(FisheryCategoryInterface.PER_DAY).getEffect();
            boolean breakStreak = userBean.getDailyStreak() != 0 && !userBean.getDailyReceived().plus(1, ChronoUnit.DAYS).equals(LocalDate.now());
            userBean.updateDailyReceived();

            int bonusCombo = 0;
            int bonusDonation = 0;
            long dailyStreakNow = breakStreak ? 1 : userBean.getDailyStreak() + 1;

            if (dailyStreakNow >= 5) {
                bonusCombo = (int) Math.round(fishes * 0.25);
            }

            if (PatreonCache.getInstance().getPatreonLevel(event.getMessageAuthor().getId()) > 1) {
                bonusDonation = (int) Math.round((fishes + bonusCombo) * 0.5);
            }

            StringBuilder sb = new StringBuilder(getString("point_default", StringUtil.numToString(getLocale(), fishes)));
            if (bonusCombo > 0)
                sb.append("\n").append(getString("point_combo", StringUtil.numToString(getLocale(), bonusCombo)));
            if (bonusDonation > 0)
                sb.append("\n").append(getString("point_donation", StringUtil.numToString(getLocale(), bonusDonation)));

            EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("codeblock", sb.toString()));
            eb.addField(getString("didyouknow_title"), getString("didyouknow_desc", ExternalLinks.PATREON_PAGE, ExternalLinks.UPVOTE_URL), false);
            if (breakStreak) EmbedFactory.addLog(eb, LogStatus.LOSE, getString("combobreak"));

            event.getChannel().sendMessage(eb).get();
            event.getChannel().sendMessage(userBean.changeValues(fishes + bonusCombo + bonusDonation, 0, dailyStreakNow)).get();

            return true;
        } else {
            Instant nextDaily = TimeUtil.setInstantToNextDay(Instant.now());

            EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("claimed_desription"));
            eb.setColor(EmbedFactory.FAILED_EMBED_COLOR);

            EmbedFactory.addLog(eb, LogStatus.TIME, TextManager.getString(getLocale(), TextManager.GENERAL, "next", TimeUtil.getRemainingTimeString(getLocale(), Instant.now(), nextDaily, false)));
            event.getChannel().sendMessage(eb).get();
            return false;
        }
    }
}
