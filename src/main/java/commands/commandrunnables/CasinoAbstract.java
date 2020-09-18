package commands.commandrunnables;

import commands.commandlisteners.OnReactionAddListener;
import commands.Command;
import commands.CommandManager;
import constants.Category;
import constants.FisheryStatus;
import constants.Settings;
import core.EmbedFactory;
import core.mention.MentionUtil;
import core.TextManager;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryUserBean;
import mysql.modules.gamestatistics.DBGameStatistics;
import mysql.modules.gamestatistics.GameStatisticsBean;
import mysql.modules.server.DBServer;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public abstract class CasinoAbstract extends Command implements OnReactionAddListener {

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

    public CasinoAbstract(Locale locale, String prefix) {
        super(locale, prefix);
        allowBet = true;
    }

    protected boolean onGameStart(MessageCreateEvent event, String followedString) throws IOException, ExecutionException, InterruptedException {
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

        FisheryUserBean userBean = DBFishery.getInstance().getBean(event.getServer().get().getId()).getUserBean(event.getMessageAuthor().getId());
        long coins = userBean.getCoins();
        long value = Math.min(MentionUtil.getAmountExt(followedString, coins), coins);
        if (value == -1) {
            coinsInput = (long) Math.ceil(coins * 0.1);
            userBean.addHiddenCoins(coinsInput);
            return true;
        }

        if (value >= 0) {
            coinsInput = value;
            userBean.addHiddenCoins(coinsInput);
            return true;
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "0"))).get();
        }

        return false;
    }

    protected void onGameEnd() throws ExecutionException {
        won = false;
        active = false;
        DBFishery.getInstance().getBean(server.getId()).getUserBean(player.getId()).addHiddenCoins(-coinsInput);
        removeNavigation();
        removeMessageForwarder();
        removeReactionListener(((OnReactionAddListener)this).getReactionMessage());
    }

    protected void onLose() throws ExecutionException {
        onGameEnd();
        if (coinsInput > 0 && useCalculatedMultiplicator) {
            DBGameStatistics.getInstance().getBean(compareKey).addValue(false, 1);
        }
        EmbedBuilder eb = DBFishery.getInstance().getBean(server.getId()).getUserBean(player.getId()).changeValues(0, -coinsInput);
        if (coinsInput > 0) channel.sendMessage(eb);
    }

    protected void onWin() throws ExecutionException {
        onGameEnd();
        won = true;

        long coinsWon = (long) Math.ceil(coinsInput * winMultiplicator);

        double multiplicator = 1;
        if (coinsInput != 0 && useCalculatedMultiplicator) {
            GameStatisticsBean gameStatisticsBean = DBGameStatistics.getInstance().getBean(compareKey);
            gameStatisticsBean.addValue(true, winMultiplicator);

            double won = gameStatisticsBean.getValue(true), lost = gameStatisticsBean.getValue(false);
            if (won > 0 && lost > 0) multiplicator = lost / won;
        }

        EmbedBuilder eb = DBFishery.getInstance().getBean(server.getId()).getUserBean(player.getId()).changeValues(0, (long) Math.ceil(coinsWon * multiplicator * BONUS_MULTIPLICATOR));
        if (coinsInput > 0) channel.sendMessage(eb);
    }

    protected EmbedBuilder addRetryOption(EmbedBuilder eb) {
        addReactionListener(getReactionMessage());
        message.addReaction(RETRY_EMOJI);
        eb.addField(Settings.EMPTY_EMOJI, TextManager.getString(getLocale(), Category.CASINO, "casino_retry", RETRY_EMOJI));
        return eb;
    }

    protected void onReactionAddRetry(SingleReactionEvent event) throws InstantiationException, IllegalAccessException, InterruptedException, ExecutionException, IOException, SQLException, InvocationTargetException {
        if (!active && event.getEmoji().isUnicodeEmoji() && event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(RETRY_EMOJI)) {
            removeReactionListener(getReactionMessage());

            Command command = CommandManager.createCommandByClass(this.getClass(), getLocale(), getPrefix());
            command.setReactionUserID(event.getUser().getId());
            command.blockLoading();

            CommandManager.manage(createEvent, command, String.valueOf(coinsInput), Instant.now());
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
