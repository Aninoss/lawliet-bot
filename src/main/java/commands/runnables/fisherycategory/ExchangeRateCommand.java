package commands.runnables.fisherycategory;

import java.time.Instant;
import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import constants.TrackerResult;
import core.EmbedFactory;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import modules.ExchangeRate;
import modules.Fishery;
import mysql.modules.tracker.TrackerSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "exch",
        emoji = "\uD83D\uDD01",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        executableWithoutArgs = true,
        aliases = { "exchangerate", "er", "exchr", "exchange" }
)
public class ExchangeRateCommand extends Command implements OnAlertListener {

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
        return EmbedFactory.getEmbedDefault(this, getString("template", StringUtil.numToString(ExchangeRate.getInstance().get(0)), Fishery.getChangeEmoji()));
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerSlot slot) throws Throwable {
        long messageId = slot.sendMessage(getEmbed().build()).get();
        slot.setMessageId(messageId);
        slot.setNextRequest(TimeUtil.setInstantToNextDay(Instant.now()).plusSeconds(10));

        return TrackerResult.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

}
