package Commands.FisheryCategory;

import CommandListeners.*;
import CommandSupporters.Command;
import Constants.Permission;
import Constants.FisheryStatus;
import Constants.Response;
import General.*;
import General.Fishing.FishingProfile;
import MySQL.DBServerOld;
import MySQL.DBUser;
import MySQL.Server.DBServer;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ExecutionException;

@CommandProperties(
    trigger = "sell",
    botPermissions = Permission.USE_EXTERNAL_EMOJIS_IN_TEXT_CHANNEL,
    thumbnail = "http://icons.iconarchive.com/icons/graphicloads/flat-finance/128/dollar-rotation-icon.png",
    emoji = "\uD83D\uDCE4",
    executable = true
)
public class SellCommand extends Command implements onRecievedListener, onReactionAddListener, onForwardedRecievedListener {

    private Message message;

    public SellCommand() {
        super();
    }

    @Override
    public boolean onReceived(MessageCreateEvent event, String followedString) throws Throwable {
        FisheryStatus status = DBServer.getInstance().getBean(event.getServer().get().getId()).getFisheryStatus();
        if (status == FisheryStatus.ACTIVE) {
            if (followedString.length() > 0) {
                return mainExecution(event, followedString);
            } else {
                FishingProfile fishingProfile = DBUser.getFishingProfile(event.getServer().get(), event.getMessage().getUserAuthor().get());
                message = event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this,
                        getString("status",
                                Tools.numToString(getLocale(), fishingProfile.getFish()),
                                Tools.numToString(getLocale(), fishingProfile.getCoins()),
                                Tools.numToString(getLocale(), ExchangeRate.getInstance().get(0)),
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
        long value = Tools.getAmountExt(argString, fishingProfile.getFish());

        if (argString.equalsIgnoreCase("no")) {
            markNoInterest(event.getServerTextChannel().get());
            return true;
        }
        if (fishingProfile.getFish() == 0) {
            sendMessage(event.getServerTextChannel().get(), EmbedFactory.getCommandEmbedError(this, getString("nofish")));
            return false;
        }
        if (value >= 1) {
            if (value <= fishingProfile.getFish()) {
                long coins = ExchangeRate.getInstance().get(0) * value;
                EmbedBuilder eb = DBUser.addFishingValues(getLocale(), event.getServer().get(), event.getMessage().getUserAuthor().get(), -value, coins);

                sendMessage(event.getServerTextChannel().get(), EmbedFactory.getCommandEmbedSuccess(this, getString("done")));
                event.getChannel().sendMessage(eb).get();
                return true;
            } else
                sendMessage(event.getServerTextChannel().get(), EmbedFactory.getCommandEmbedError(this, getString("too_large", fishingProfile.getFish() != 1, Tools.numToString(getLocale(), fishingProfile.getFish()))));
        } else if (value == 0)
            sendMessage(event.getServerTextChannel().get(), EmbedFactory.getCommandEmbedError(this, getString("nofish")));
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
    public Response onForwardedRecieved(MessageCreateEvent event) throws Throwable {
        return mainExecution(event, event.getMessage().getContent()) ? Response.TRUE : Response.FALSE;
    }

    @Override
    public Message getForwardedMessage() {
        return message;
    }

    @Override
    public void onForwardedTimeOut() {}

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
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (event.getEmoji().isUnicodeEmoji() && event.getEmoji().asUnicodeEmoji().get().equals("❌")) {
            markNoInterest(event.getServerTextChannel().get());
        }
    }

    @Override
    public void onNewActivityOverwrite() {
        removeNavigation();
        removeReactionListener();
    }

    private void markNoInterest(ServerTextChannel channel) throws IOException, ExecutionException, InterruptedException, InvalidKeySpecException, NoSuchAlgorithmException {
        removeMessageForwarder();
        removeReactionListener();
        sendMessage(channel, EmbedFactory.getCommandEmbedError(this, getString("nointerest_description", Tools.numToString(getLocale(), ExchangeRate.getInstance().get(0)), getChangeEmoji()), getString("nointerest_title")));
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) {}

}
