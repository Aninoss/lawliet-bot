package commands.runnables.fisherycategory;

import java.time.Instant;
import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnTrackerRequestListener;
import constants.TrackerResult;
import core.EmbedFactory;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import modules.ExchangeRate;
import mysql.modules.tracker.TrackerBeanSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "exch",
        emoji = "\uD83D\uDD01",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        executableWithoutArgs = true,
        aliases = { "exchangerate", "er", "exchr", "exchange" }
)
public class ExchangeRateCommand extends Command implements OnTrackerRequestListener {

    public ExchangeRateCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        EmbedBuilder eb = getEmbed();
        EmbedUtil.addTrackerNoteLog(getLocale(), event.getMember(), eb, getPrefix(), getTrigger());
        event.getChannel().sendMessage(eb.build()).queue();
        return true;
    }

    private EmbedBuilder getEmbed() {
        return EmbedFactory.getEmbedDefault(this, getString("template", StringUtil.numToString(ExchangeRate.getInstance().get(0)), getChangeEmoji()));
    }

    private String getChangeEmoji() {
        int rateNow = ExchangeRate.getInstance().get(0);
        int rateBefore = ExchangeRate.getInstance().get(-1);

        if (rateNow > rateBefore) {
            return "\uD83D\uDD3A";
        } else {
            if (rateNow < rateBefore) {
                return "\uD83D\uDD3B";
            } else {
                return "•";
            }
        }
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        Message message = slot.getTextChannel().get().sendMessage(getEmbed().build()).complete();
        slot.setMessageId(message.getIdLong());
        slot.setNextRequest(TimeUtil.setInstantToNextDay(Instant.now()).plusSeconds(10));

        return TrackerResult.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

}
