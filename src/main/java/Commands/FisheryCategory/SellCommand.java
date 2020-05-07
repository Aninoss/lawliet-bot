package Commands.FisheryCategory;

import CommandListeners.*;
import CommandSupporters.Command;
import Commands.FisheryAbstract;
import Constants.Permission;
import Constants.FisheryStatus;
import Constants.Response;
import Core.*;
import Modules.ExchangeRate;
import Core.Mention.MentionUtil;
import Core.Utils.StringUtil;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryUserBean;
import MySQL.Modules.Server.DBServer;
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
    botPermissions = Permission.USE_EXTERNAL_EMOJIS,
    thumbnail = "http://icons.iconarchive.com/icons/graphicloads/flat-finance/128/dollar-rotation-icon.png",
    emoji = "\uD83D\uDCE4",
    executable = true
)
public class SellCommand extends FisheryAbstract implements OnReactionAddListener, OnForwardedRecievedListener {

    private Message message;
    private FisheryUserBean userBean;

    @Override
    public boolean onMessageReceivedSuccessful(MessageCreateEvent event, String followedString) throws Throwable {
        userBean = DBFishery.getInstance().getBean(event.getServer().get().getId()).getUserBean(event.getMessageAuthor().getId());
        if (followedString.length() > 0) {
            return mainExecution(event, followedString);
        } else {
            message = event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this,
                    getString("status",
                            StringUtil.numToString(getLocale(), userBean.getFish()),
                            StringUtil.numToString(getLocale(), userBean.getCoins()),
                            StringUtil.numToString(getLocale(), ExchangeRate.getInstance().get(0)),
                            getChangeEmoji()
                    ))).get();
            message.addReaction("❌");
            return true;
        }
    }

    private boolean mainExecution(MessageCreateEvent event, String argString) throws Throwable {
        removeReactionListener(message);
        removeMessageForwarder();
        long value = MentionUtil.getAmountExt(argString, userBean.getFish());

        if (argString.equalsIgnoreCase("no")) {
            markNoInterest(event.getServerTextChannel().get());
            return true;
        }
        if (userBean.getFish() == 0) {
            sendMessage(event.getServerTextChannel().get(), EmbedFactory.getCommandEmbedError(this, getString("nofish")));
            return false;
        }
        if (value >= 1) {
            if (value <= userBean.getFish()) {
                long coins = ExchangeRate.getInstance().get(0) * value;
                EmbedBuilder eb = userBean.changeValues(-value, coins);

                sendMessage(event.getServerTextChannel().get(), EmbedFactory.getCommandEmbedSuccess(this, getString("done")));
                event.getChannel().sendMessage(eb).get();
                return true;
            } else
                sendMessage(event.getServerTextChannel().get(), EmbedFactory.getCommandEmbedError(this, getString("too_large", userBean.getFish() != 1, StringUtil.numToString(getLocale(), userBean.getFish()))));
        } else if (value == 0)
            sendMessage(event.getServerTextChannel().get(), EmbedFactory.getCommandEmbedError(this, getString("nofish")));
        else if (value == -1)
            sendMessage(event.getServerTextChannel().get(), EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit")));
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
        sendMessage(channel, EmbedFactory.getCommandEmbedError(this, getString("nointerest_description", StringUtil.numToString(getLocale(), ExchangeRate.getInstance().get(0)), getChangeEmoji()), getString("nointerest_title")));
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) {}

}
