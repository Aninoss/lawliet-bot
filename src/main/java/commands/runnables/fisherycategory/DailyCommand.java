package commands.runnables.fisherycategory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import constants.ExternalLinks;
import constants.FisheryGear;
import constants.LogStatus;
import core.EmbedFactory;
import core.TextManager;
import core.buttons.ButtonStyle;
import core.buttons.MessageButton;
import core.buttons.MessageSendActionAdvanced;
import core.cache.PatreonCache;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "daily",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83D\uDDD3",
        executableWithoutArgs = true,
        aliases = { "d", "day" }
)
public class DailyCommand extends Command implements FisheryInterface {

    public DailyCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(GuildMessageReceivedEvent event, String args) throws Throwable {
        FisheryMemberData userBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberBean(event.getMember().getIdLong());
        if (!userBean.getDailyReceived().equals(LocalDate.now())) {
            long fish = userBean.getMemberGear(FisheryGear.DAILY).getEffect();
            boolean breakStreak = userBean.getDailyStreak() != 0 && !userBean.getDailyReceived().plus(1, ChronoUnit.DAYS).equals(LocalDate.now());
            userBean.updateDailyReceived();

            int bonusCombo = 0;
            int bonusDonation = 0;
            long dailyStreakNow = breakStreak ? 1 : userBean.getDailyStreak() + 1;

            if (dailyStreakNow >= 5) {
                bonusCombo = (int) Math.round(fish * 0.25);
            }

            if (PatreonCache.getInstance().getUserTier(event.getMember().getIdLong(), false) >= 2) {
                bonusDonation = (int) Math.round((fish + bonusCombo) * 0.5);
            }

            StringBuilder sb = new StringBuilder(getString("point_default", StringUtil.numToString(fish)));
            if (bonusCombo > 0) {
                sb.append("\n").append(getString("point_combo", StringUtil.numToString(bonusCombo)));
            }
            if (bonusDonation > 0) {
                sb.append("\n").append(getString("point_donation", StringUtil.numToString(bonusDonation)));
            }

            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("codeblock", sb.toString()));
            eb.addField(getString("didyouknow_title"), getString("didyouknow_desc"), false);
            if (breakStreak) EmbedUtil.addLog(eb, LogStatus.LOSE, getString("combobreak"));

            new MessageSendActionAdvanced(event.getChannel())
                    .appendButtons(new MessageButton(ButtonStyle.LINK, getString("upvote"), ExternalLinks.UPVOTE_URL))
                    .appendButtons(new MessageButton(ButtonStyle.LINK, getString("patreon"), ExternalLinks.PATREON_PAGE))
                    .embed(eb.build())
                    .queue();
            event.getChannel().sendMessage(userBean.changeValuesEmbed(fish + bonusCombo + bonusDonation, 0, dailyStreakNow).build()).queue();

            return true;
        } else {
            Instant nextDaily = TimeUtil.setInstantToNextDay(Instant.now());

            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("claimed_desription"));
            eb.setColor(EmbedFactory.FAILED_EMBED_COLOR);

            EmbedUtil.addRemainingTime(eb, nextDaily);
            EmbedUtil.addLog(eb, LogStatus.TIME, TextManager.getString(getLocale(), TextManager.GENERAL, "next", TimeUtil.getRemainingTimeString(getLocale(), Instant.now(), nextDaily, false)));
            event.getChannel().sendMessage(eb.build()).queue();
            return false;
        }
    }

}
