package commands.runnables.fisherycategory;

import commands.listeners.*;
import commands.Command;
import constants.TrackerResult;
import core.*;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import modules.ExchangeRate;
import mysql.modules.tracker.TrackerBeanSlot;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.Locale;

@CommandProperties(
        trigger = "exch",
        emoji = "\uD83D\uDD01",
        executableWithoutArgs = true,
        aliases = {"exchangerate", "er", "exchr"}
)
public class ExchangeRateCommand extends Command implements OnTrackerRequestListener {

    public ExchangeRateCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        EmbedBuilder eb = getEmbed();
        EmbedFactory.addTrackerNoteLog(getLocale(), event.getServer().get(), event.getMessage().getUserAuthor().get(), eb, getPrefix(), getTrigger());
        event.getChannel().sendMessage(eb).get();
        return true;
    }

    private EmbedBuilder getEmbed() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        return EmbedFactory.getCommandEmbedStandard(this, getString("template", StringUtil.numToString(ExchangeRate.getInstance().get(0)), getChangeEmoji()));
    }

    private String getChangeEmoji() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        int rateNow = ExchangeRate.getInstance().get(0);
        int rateBefore = ExchangeRate.getInstance().get(-1);

        if (rateNow > rateBefore) return "\uD83D\uDD3A";
        else {
            if (rateNow < rateBefore) return "\uD83D\uDD3B";
            else return "•";
        }
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerBeanSlot slot) throws Throwable {
        slot.getMessage().ifPresent(Message::delete);
        Message message = slot.getChannel().get().sendMessage(getEmbed()).get();
        slot.setMessageId(message.getId());
        slot.setNextRequest(TimeUtil.setInstantToNextDay(Instant.now()).plusSeconds(10));

        return TrackerResult.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

}
