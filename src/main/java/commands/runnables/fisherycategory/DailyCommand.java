package commands.runnables.fisherycategory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import constants.Emojis;
import constants.ExternalLinks;
import modules.fishery.FisheryGear;
import constants.LogStatus;
import core.EmbedFactory;
import core.cache.PatreonCache;
import core.components.ActionRows;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.utils.TimeFormat;

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
    public boolean onFisheryAccess(GuildMessageReceivedEvent event, String args) throws Throwable {
        FisheryMemberData userBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberData(event.getMember().getIdLong());
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

            List<ActionRow> rows = ActionRows.of(
                    Button.of(ButtonStyle.LINK, ExternalLinks.UPVOTE_URL, getString("upvote")),
                    Button.of(ButtonStyle.LINK, ExternalLinks.PATREON_PAGE, getString("patreon"))
            );

            MessageEmbed userChangeValueEmbed = userBean.changeValuesEmbed(event.getMember(), fish + bonusCombo + bonusDonation, 0, dailyStreakNow).build();
            event.getChannel().sendMessageEmbeds(eb.build(), userChangeValueEmbed)
                    .setActionRows(rows)
                    .queue();

            return true;
        } else {
            Instant nextDaily = TimeUtil.setInstantToNextDay(Instant.now());

            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("claimed_desription"));
            eb.setColor(EmbedFactory.FAILED_EMBED_COLOR);

            eb.addField(Emojis.ZERO_WIDTH_SPACE, getString("next", TimeFormat.DATE_TIME_SHORT.atInstant(nextDaily).toString()), false);
            event.getChannel().sendMessageEmbeds(eb.build()).queue();
            return false;
        }
    }

}
