package Commands.FisheryCategory;

import CommandListeners.*;
import CommandSupporters.Command;
import Constants.LogStatus;
import Constants.Permission;
import Constants.PowerPlantStatus;
import Constants.Response;
import General.EmbedFactory;
import General.ExchangeRate;
import General.Fishing.FishingProfile;
import General.TextManager;
import General.Tools;
import General.Tracker.TrackerData;
import MySQL.DBMain;
import MySQL.DBServer;
import MySQL.DBUser;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "exch",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/flat-finance/128/dollar-rotation-icon.png",
        emoji = "\uD83D\uDD01",
        executable = true,
        aliases = {"exchangerate", "er", "exchr"}
)
public class ExchangeRateCommand extends Command implements onRecievedListener, onTrackerRequestListener {

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        EmbedBuilder eb = getEmbed();
        EmbedFactory.addLog(eb, LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "tracker", getPrefix(), getTrigger()));
        event.getChannel().sendMessage(eb).get();
        return true;
    }

    private EmbedBuilder getEmbed() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        return EmbedFactory.getCommandEmbedStandard(this, getString("template", Tools.numToString(getLocale(), ExchangeRate.getInstance().get(0)), getChangeEmoji()));
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
    public TrackerData onTrackerRequest(TrackerData trackerData) throws Throwable {
        if (trackerData.getMessageDelete() != null) trackerData.getMessageDelete().delete();
        Message message = trackerData.getChannel().sendMessage(getEmbed()).get();
        trackerData.setMessageDelete(message);
        trackerData.setInstant(Tools.setInstantToNextDay(Instant.now()).plusSeconds(10));
        return trackerData;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

    @Override
    public boolean needsPrefix() {
        return false;
    }
}
