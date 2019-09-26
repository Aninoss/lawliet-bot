package Commands.PowerPlant;

import CommandListeners.*;
import CommandSupporters.Command;
import Constants.Permission;
import Constants.PowerPlantStatus;
import Constants.Settings;
import General.*;
import General.Fishing.FishingProfile;
import MySQL.DBServer;
import MySQL.DBUser;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.ExecutionException;

@CommandProperties(
    trigger = "sell",
    botPermissions = Permission.USE_EXTERNAL_EMOJIS_IN_TEXT_CHANNEL,
    thumbnail = "http://icons.iconarchive.com/icons/graphicloads/flat-finance/128/dollar-rotation-icon.png",
    emoji = "\uD83D\uDCE4",
    executable = true
)
public class SellCommand extends Command implements onRecievedListener, onReactionAddListener, onForwardedRecievedListener {
    private static int COINS_PER_FISH[] = {-1, -1};
    private Message message;

    public SellCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        PowerPlantStatus status = DBServer.getPowerPlantStatusFromServer(event.getServer().get());
        if (status == PowerPlantStatus.ACTIVE) {
            if (followedString.length() > 0) {
                return mainExecution(event, followedString);
            } else {
                FishingProfile fishingProfile = DBUser.getFishingProfile(event.getServer().get(), event.getMessage().getUserAuthor().get());
                message = event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this,
                        getString("status",
                                Tools.numToString(getLocale(), fishingProfile.getFish()),
                                Tools.numToString(getLocale(), fishingProfile.getCoins()),
                                Tools.numToString(getLocale(), getExchangeRate(0)),
                                getChangeEmoji()
                        ))).get();
                message.addReaction("❌");
                return true;
            }
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title")));
            return false;
        }
    }

    private boolean mainExecution(MessageCreateEvent event, String argString) throws Throwable {
        removeReactionListener(message);
        removeMessageForwarder();
        FishingProfile fishingProfile = DBUser.getFishingProfile(event.getServer().get(), event.getMessage().getUserAuthor().get());
        long value = Tools.filterNumberFromString(argString);

        if (argString.equalsIgnoreCase("no")) {
            markNoInterest(event.getServerTextChannel().get());
            return true;
        }

        if (argString.toLowerCase().contains("all")) {
            value = fishingProfile.getFish();
            if (value == 0) {
                sendMessage(event.getServerTextChannel().get(), EmbedFactory.getCommandEmbedError(this, getString("nofish")));
                return false;
            }
        }
        if (value >= 1) {
            if (value <= fishingProfile.getFish()) {
                long fish = value;
                long coins = getExchangeRate(0) * value;
                EmbedBuilder eb = DBUser.addFishingValues(getLocale(), event.getServer().get(), event.getMessage().getUserAuthor().get(), -fish, coins);

                sendMessage(event.getServerTextChannel().get(), EmbedFactory.getCommandEmbedSuccess(this, getString("done")));
                event.getChannel().sendMessage(eb).get();
                return true;
            } else
                sendMessage(event.getServerTextChannel().get(), EmbedFactory.getCommandEmbedError(this, getString("too_large", fishingProfile.getFish() != 1, Tools.numToString(getLocale(), fishingProfile.getFish()))));
        } else if (value == 0)
            sendMessage(event.getServerTextChannel().get(), EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1")));
        else if (value == -1)
            sendMessage(event.getServerTextChannel().get(), EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit")));
        return false;
    }

    private void sendMessage(ServerTextChannel channel, EmbedBuilder embedBuilder) throws ExecutionException, InterruptedException {
        if (message != null) message.edit(embedBuilder).get();
        else channel.sendMessage(embedBuilder).get();
        message = null;
    }

    @Override
    public boolean onForwardedRecieved(MessageCreateEvent event) throws Throwable {
        return mainExecution(event, event.getMessage().getContent());
    }

    @Override
    public Message getForwardedMessage() {
        return message;
    }

    @Override
    public void onForwardedTimeOut() {}

    private int getExchangeRate(int daysAdd) {
        if (COINS_PER_FISH[daysAdd + 1] == -1) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, daysAdd);

            String date = String.valueOf(calendar.get(Calendar.DAY_OF_YEAR));
            int root = Security.getHashForString(date, date).hashCode();
            Random r = new Random(root);

            double result = r.nextDouble();
            for (int i = 0; i < 2; i++) {
                double d = r.nextDouble();
                if (Math.abs(d - 0.5) < Math.abs(result - 0.5)) result = d;
            }

            COINS_PER_FISH[daysAdd + 1] = (int) Math.round(Settings.ONE_CURRENCY_TO_COINS * (0.5 + result * 1.0));
        }

        return COINS_PER_FISH[daysAdd + 1];
    }

    private String getChangeEmoji() {
        int rateNow = getExchangeRate(0);
        int rateBefore = getExchangeRate(-1);

        if (rateNow > rateBefore) return "\uD83D\uDD3A";
        else {
            if (rateNow < rateBefore) return "\uD83D\uDD3B";
            else return "•";
        }
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (event.getEmoji().isUnicodeEmoji() && event.getEmoji().asUnicodeEmoji().get().equals("❌")) {
            markNoInterest(event.getServerTextChannel().get());
        }
    }

    private void markNoInterest(ServerTextChannel channel) throws IOException, ExecutionException, InterruptedException {
        removeMessageForwarder();
        removeReactionListener();
        sendMessage(channel, EmbedFactory.getCommandEmbedError(this, getString("nointerest_description", Tools.numToString(getLocale(), getExchangeRate(0)), getChangeEmoji()), getString("nointerest_title")));
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) {}

    public static void resetCoinsPerFish() {
        COINS_PER_FISH = new int[]{-1, -1};
    }
}
