package Commands;

import CommandListeners.*;
import CommandSupporters.Command;
import CommandSupporters.CommandManager;
import CommandSupporters.RunningCommands.RunningCommand;
import Constants.FisheryStatus;
import Constants.Settings;
import General.CasinoBetContainer;
import General.EmbedFactory;
import General.Mention.MentionTools;
import CommandSupporters.RunningCommands.RunningCommandManager;
import General.TextManager;
import General.Tools.StringTools;
import MySQL.DBUser;
import MySQL.GameStatistics.DBGameStatistics;
import MySQL.GameStatistics.GameStatisticsBean;
import MySQL.Server.DBServer;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public abstract class CasinoAbstract extends Command implements onReactionAddListener {

    protected long coinsInput;
    protected User player;
    protected Server server;
    protected ServerTextChannel channel;
    protected double winMultiplicator;
    protected boolean active, won, useCalculatedMultiplicator, allowBet, onlyNumbersAsArg = true;
    protected final double BONUS_MULTIPLICATOR = 1;
    protected String compareKey;
    protected Message message;

    private MessageCreateEvent createEvent;
    private final String RETRY_EMOJI = "\uD83D\uDD01";

    public CasinoAbstract() {
        super();
        allowBet = true;
    }

    protected boolean onGameStart(MessageCreateEvent event, String followedString) throws SQLException, IOException, ExecutionException, InterruptedException {
        createEvent = event;
        server = event.getServer().get();
        player = event.getMessage().getUserAuthor().get();
        channel = event.getServerTextChannel().get();
        active = true;
        useCalculatedMultiplicator = true;
        compareKey = getTrigger();

        if (!allowBet) {
            coinsInput = 0;
            return true;
        }

        FisheryStatus status = DBServer.getInstance().getBean(event.getServer().get().getId()).getFisheryStatus();
        if (status != FisheryStatus.ACTIVE) {
            coinsInput = 0;
            return true;
        }

        long coins = DBUser.getFishingProfile(server, player).getCoins();
        long value = MentionTools.getAmountExt(followedString, coins);
        if (value == -1) {
            coinsInput = (long) Math.ceil(coins * 0.1);
            CasinoBetContainer.getInstance().addBet(player, coinsInput);
            return true;
        }

        if (value >= 0) {
            if (value <= coins) {
                coinsInput = value;
                CasinoBetContainer.getInstance().addBet(player, coinsInput);
                return true;
            } else {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.COMMANDS, "casino_too_large", StringTools.numToString(getLocale(), coins)))).get();
            }
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "0"))).get();
        }

        return false;
    }

    protected void onGameEnd() {
        won = false;
        active = false;
        CasinoBetContainer.getInstance().removeBet(player, coinsInput);
        removeNavigation();
        removeMessageForwarder();
        removeReactionListener(((onReactionAddListener)this).getReactionMessage());
    }

    protected void onLose() throws SQLException, IOException, ExecutionException {
        onGameEnd();
        if (coinsInput > 0 && useCalculatedMultiplicator) {
            DBGameStatistics.getInstance().getBean(compareKey).addValue(false, Math.pow(coinsInput, 0.25));
        }
        EmbedBuilder eb = DBUser.addFishingValues(getLocale(), server, player, 0, -coinsInput);
        if (coinsInput > 0) channel.sendMessage(eb);
    }

    protected void onWin() throws IOException, SQLException, ExecutionException {
        onGameEnd();
        won = true;

        long coinsWon = (long) Math.ceil(coinsInput * winMultiplicator);

        double multiplicator = 1;
        if (coinsInput != 0 && useCalculatedMultiplicator) {
            GameStatisticsBean gameStatisticsBean = DBGameStatistics.getInstance().getBean(compareKey);
            gameStatisticsBean.addValue(true, winMultiplicator * Math.pow(coinsInput, 0.25));

            double won = gameStatisticsBean.getValue(true), lost = gameStatisticsBean.getValue(false);
            if (won > 0 && lost > 0) multiplicator = lost / won;
        }

        EmbedBuilder eb = DBUser.addFishingValues(getLocale(), server, player, 0, (long) Math.ceil(coinsWon * multiplicator * BONUS_MULTIPLICATOR));
        if (coinsInput > 0) channel.sendMessage(eb);
    }

    protected EmbedBuilder addRetryOption(EmbedBuilder eb) throws IOException {
        addReactionListener(getReactionMessage());
        message.addReaction(RETRY_EMOJI);
        eb.addField(Settings.EMPTY_EMOJI, TextManager.getString(getLocale(), TextManager.COMMANDS, "casino_retry", RETRY_EMOJI));
        return eb;
    }

    protected void onReactionAddRetry(SingleReactionEvent event) throws InstantiationException, IllegalAccessException, InterruptedException, ExecutionException, IOException, SQLException {
        if (!active && event.getEmoji().isUnicodeEmoji() && event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(RETRY_EMOJI)) {
            removeReactionListener(getReactionMessage());

            Command command = CommandManager.createCommandByClass(this.getClass(), getLocale(), getPrefix());
            command.setReactionUserID(event.getUser().getId());
            command.blockLoading();

            CommandManager.manage(createEvent, command, String.valueOf(coinsInput));
        }
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        onReactionAddRetry(event);
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable {}

}
