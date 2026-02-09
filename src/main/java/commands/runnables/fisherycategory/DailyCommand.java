package commands.runnables.fisherycategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import constants.Emojis;
import constants.ExternalLinks;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.cache.PatreonCache;
import core.components.ActionRows;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import modules.fishery.FisheryGear;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "daily",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83D\uDDD3",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "d", "day" }
)
public class DailyCommand extends Command implements FisheryInterface {

    public DailyCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(CommandEvent event, String args) throws Throwable {
        FisheryMemberData userBean = FisheryUserManager.getGuildData(event.getGuild().getIdLong()).getMemberData(event.getMember().getIdLong());
        if (!userBean.getDailyReceived().equals(LocalDate.now())) {
            long fish = userBean.getMemberGear(FisheryGear.DAILY).getEffect();
            boolean breakStreak = userBean.getDailyStreak() != 0 && !userBean.getDailyReceived().plus(1, ChronoUnit.DAYS).equals(LocalDate.now());
            userBean.updateDailyReceived();

            long bonusCombo = 0;
            long bonusDonation = 0;
            long dailyStreakNow = breakStreak ? 1 : userBean.getDailyStreak() + 1;

            if (dailyStreakNow >= 5) {
                bonusCombo = Math.round(fish * 0.25);
            }

            if (PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), false)) {
                bonusDonation = Math.round((fish + bonusCombo) * 0.5);
            }

            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("codeblock", getValueContent(fish, bonusCombo, bonusDonation)));
            eb.addField(getString("didyouknow_title"), getString("didyouknow_desc"), false);
            if (breakStreak) EmbedUtil.addLog(eb, LogStatus.LOSE, getString("combobreak"));

            List<ActionRow> rows = ActionRows.of(
                    Button.of(ButtonStyle.LINK, ExternalLinks.UPVOTE_URL, getString("upvote")),
                    Button.of(ButtonStyle.LINK, ExternalLinks.PREMIUM_WEBSITE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_button_unlock"))
            );

            MessageEmbed userChangeValueEmbed = userBean.changeValuesEmbed(event.getMember(), fish + bonusCombo + bonusDonation, 0, dailyStreakNow, getGuildEntity()).build();
            setActionRows(rows);
            setAdditionalEmbeds(userChangeValueEmbed);
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());

            return true;
        } else {
            Instant nextDaily = TimeUtil.setInstantToNextDay(Instant.now());

            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("claimed_desription"));
            eb.setColor(EmbedFactory.FAILED_EMBED_COLOR);

            eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), getString("next", TimeFormat.DATE_TIME_SHORT.atInstant(nextDaily).toString()), false);
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }
    }

    private String getValueContent(long fish, long bonusCombo, long bonusDonation) {
        ArrayList<String> valueContentLines = new ArrayList<>();
        ArrayList<Long> values = new ArrayList<>();

        valueContentLines.add(getString("point_default"));
        values.add(fish);
        if (bonusCombo > 0) {
            valueContentLines.add(getString("point_combo"));
            values.add(bonusCombo);
        }
        if (bonusDonation > 0) {
            valueContentLines.add(getString("point_donation"));
            values.add(bonusDonation);
        }
        int maxLength = valueContentLines.stream().mapToInt(String::length).max().getAsInt();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(values.size(), valueContentLines.size()); i++) {
            String line = valueContentLines.get(i);
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append(line + " ".repeat(2 + maxLength - line.length()) + getString("point_add", StringUtil.numToString(values.get(i))));
        }
        return sb.toString();
    }

}
