package Commands.FisheryCategory;

import CommandListeners.*;
import CommandSupporters.Command;
import Constants.LogStatus;
import Constants.TrackerResult;
import Core.*;
import Core.Utils.StringUtil;
import Core.Utils.TimeUtil;
import Modules.ExchangeRate;
import MySQL.Modules.Tracker.TrackerBeanSlot;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;

@CommandProperties(
        trigger = "exch",
        emoji = "\uD83D\uDD01",
        executable = true,
        aliases = {"exchangerate", "er", "exchr"}
)
public class ExchangeRateCommand extends Command implements OnTrackerRequestListener {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        EmbedBuilder eb = getEmbed();
        EmbedFactory.addTrackerNote(getLocale(), eb, getPrefix(), getTrigger());
        event.getChannel().sendMessage(eb).get();
        return true;
    }

    private EmbedBuilder getEmbed() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        return EmbedFactory.getCommandEmbedStandard(this, getString("template", StringUtil.numToString(getLocale(), ExchangeRate.getInstance().get(0)), getChangeEmoji()));
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
