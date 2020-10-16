package commands.runnables.fisherycategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnForwardedRecievedListener;
import commands.listeners.OnReactionAddListener;
import commands.runnables.FisheryAbstract;
import constants.Permission;
import constants.Response;
import core.EmbedFactory;
import core.utils.MentionUtil;
import core.TextManager;
import core.utils.StringUtil;
import modules.ExchangeRate;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryUserBean;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
    trigger = "sell",
    botPermissions = Permission.USE_EXTERNAL_EMOJIS,
    emoji = "\uD83D\uDCE4",
    executableWithoutArgs = true
)
public class SellCommand extends FisheryAbstract implements OnReactionAddListener, OnForwardedRecievedListener {

    private Message message;
    private FisheryUserBean userBean;

    public SellCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceivedSuccessful(MessageCreateEvent event, String followedString) throws Throwable {
        userBean = DBFishery.getInstance().getBean(event.getServer().get().getId()).getUserBean(event.getMessageAuthor().getId());
        if (followedString.length() > 0) {
            return mainExecution(event, followedString);
        } else {
            message = event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this,
                    getString("status",
                            StringUtil.numToString(userBean.getFish()),
                            StringUtil.numToString(userBean.getCoins()),
                            StringUtil.numToString(ExchangeRate.getInstance().get(0)),
                            getChangeEmoji()
                    ))).get();
            message.addReaction("❌");
            return true;
        }
    }

    private boolean mainExecution(MessageCreateEvent event, String argString) throws Throwable {
        removeReactionListener(message);
        removeMessageForwarder();
        long value = Math.min(MentionUtil.getAmountExt(argString, userBean.getFish()), userBean.getFish());

        if (argString.equalsIgnoreCase("no")) {
            markNoInterest(event.getServerTextChannel().get());
            return true;
        }

        if (value >= 1) {
            long coins = ExchangeRate.getInstance().get(0) * value;
            EmbedBuilder eb = userBean.changeValues(-value, coins);

            sendMessage(event.getServerTextChannel().get(), EmbedFactory.getEmbedDefault(this, getString("done")));
            event.getChannel().sendMessage(eb).get();
            return true;
        } else if (value == 0) {
            if (userBean.getFish() <= 0)
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("nofish"))).get();
            else
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1"))).get();
        } else if (value == -1) {
            sendMessage(event.getServerTextChannel().get(), EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit")));
        }

        return false;
    }

    private void sendMessage(ServerTextChannel channel, EmbedBuilder embedBuilder) throws ExecutionException, InterruptedException {
        if (message != null) message.edit(embedBuilder);
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

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("nointerest_description", StringUtil.numToString(ExchangeRate.getInstance().get(0)), getChangeEmoji()));
        sendMessage(channel, eb);
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) {}

}
