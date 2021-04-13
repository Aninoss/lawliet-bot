package commands.runnables;

import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.OnMessageInputListener;
import commands.listeners.OnReactionListener;
import constants.*;
import core.EmbedFactory;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.EmojiUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberBean;
import mysql.modules.gamestatistics.DBGameStatistics;
import mysql.modules.gamestatistics.GameStatisticsBean;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

public abstract class CasinoAbstract extends Command implements OnReactionListener, OnMessageInputListener {

    public enum Status { ACTIVE, WON, LOST, DRAW, CANCELED }

    private final double BONUS_MULTIPLICATOR = 1;
    private final String RETRY_EMOJI = "\uD83D\uDD01";

    private String compareKey;
    private long coinsInput;
    private double winMultiplicator = 1;
    private Status status = Status.ACTIVE;
    private final boolean useCalculatedMultiplicator;
    private final boolean allowBet;
    private boolean canBeCanceled = false;
    private boolean retryRequestAdded = false;

    public CasinoAbstract(Locale locale, String prefix, boolean allowBet, boolean useCalculatedMultiplicator) {
        super(locale, prefix);
        this.compareKey = getTrigger();
        this.allowBet = allowBet;
        this.useCalculatedMultiplicator = useCalculatedMultiplicator;
    }

    public abstract String[] onGameStart(GuildMessageReceivedEvent event, String args) throws Throwable;

    public abstract boolean onReactionCasino(GenericGuildMessageReactionEvent event) throws Throwable;

    public abstract EmbedBuilder drawCasino(String playerName, long coinsInput);

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws Throwable {
        try {
            if (!allowBet) {
                setLog(LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "nobet"));
            }
            String[] emojis = onGameStart(event, args);
            if (emojis == null) {
                return false;
            }
            if (Arrays.asList(emojis).contains(Emojis.X)) {
                canBeCanceled = true;
            }

            if (!allowBet) {
                coinsInput = 0;
                registerReactionListener(emojis);
                registerMessageInputListener(false);
                return true;
            }

            FisheryStatus status = DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).getFisheryStatus();
            if (status != FisheryStatus.ACTIVE) {
                coinsInput = 0;
                registerReactionListener(emojis);
                registerMessageInputListener(false);
                return true;
            }

            FisheryMemberBean memberBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberBean(event.getMember().getIdLong());
            long coins = memberBean.getCoins();
            long value = Math.min(MentionUtil.getAmountExt(args, coins), coins);
            if (value == -1) {
                coinsInput = (long) Math.ceil(coins * 0.1);
                memberBean.addHiddenCoins(coinsInput);
                registerReactionListener(emojis);
                registerMessageInputListener(false);
                return true;
            }

            if (value >= 0) {
                coinsInput = value;
                memberBean.addHiddenCoins(coinsInput);
                registerReactionListener(emojis);
                registerMessageInputListener(false);
                return true;
            } else {
                event.getChannel()
                        .sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "0")).build())
                        .queue();
                return false;
            }
        } catch (Throwable e) {
            endGame();
            throw e;
        }
    }

    public void setCompareKey(String compareKey) {
        this.compareKey = compareKey;
    }

    public Status getStatus() {
        return status;
    }

    protected void endGame() {
        endGame(true);
    }

    protected void endGame(boolean requestRetry) {
        getGuild().ifPresent(guild -> {
            status = Status.DRAW;
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), Category.CASINO, "casino_draw"));
            DBFishery.getInstance().retrieve(guild.getIdLong()).getMemberBean(getMemberId().get()).addHiddenCoins(-coinsInput);
            deregisterListeners();
            CompletableFuture<Void> future = deregisterListenersWithReactions();
            if (requestRetry) {
                retryRequestAdded = true;
                future.thenRun(() -> registerReactionListener(RETRY_EMOJI));
            }
        });
    }

    protected void lose() {
        lose(true);
    }

    protected void lose(boolean requestRetry) {
        getGuild().ifPresent(guild -> {
            endGame(requestRetry);
            status = Status.LOST;
            setLog(LogStatus.LOSE, TextManager.getString(getLocale(), Category.CASINO, "casino_lose"));
            if (coinsInput > 0 && useCalculatedMultiplicator) {
                DBGameStatistics.getInstance().retrieve(compareKey).addValue(false, 1);
            }
            EmbedBuilder eb = DBFishery.getInstance().retrieve(guild.getIdLong()).getMemberBean(getMemberId().get())
                    .changeValuesEmbed(0, -coinsInput);
            if (coinsInput > 0) {
                getTextChannel().ifPresent(channel -> channel.sendMessage(eb.build()).queue());
            }
        });
    }

    protected void cancel(boolean loseBet, boolean requestRetry) {
        if (loseBet) {
            lose(requestRetry);
        } else {
            endGame(requestRetry);
        }
        status = Status.CANCELED;
        setLog(LogStatus.LOSE, TextManager.getString(getLocale(), Category.CASINO, "casino_abort"));
    }

    protected void win(double winMultiplicator) {
        this.winMultiplicator = winMultiplicator;
        win();
    }

    protected void win() {
        getGuild().ifPresent(guild -> {
            endGame();
            status = Status.WON;
            setLog(LogStatus.WIN, TextManager.getString(getLocale(), Category.CASINO, "casino_win"));

            long coinsWon = (long) Math.ceil(coinsInput * winMultiplicator);

            double multiplicator = 1;
            if (coinsInput != 0 && useCalculatedMultiplicator) {
                GameStatisticsBean gameStatisticsBean = DBGameStatistics.getInstance().retrieve(compareKey);
                gameStatisticsBean.addValue(true, winMultiplicator);

                double won = gameStatisticsBean.getValue(true);
                double lost = gameStatisticsBean.getValue(false);
                if (won > 0 && lost > 0) multiplicator = lost / won;
            }

            EmbedBuilder eb = DBFishery.getInstance().retrieve(guild.getIdLong()).getMemberBean(getMemberId().get())
                    .changeValuesEmbed(0, (long) Math.ceil(coinsWon * multiplicator * BONUS_MULTIPLICATOR));
            if (coinsInput > 0) {
                getTextChannel().ifPresent(channel -> channel.sendMessage(eb.build()).queue());
            }
        });
    }

    protected void disableCanceling() {
        if (canBeCanceled) {
            if (CommandContainer.getInstance().getListener(OnReactionListener.class, this).isPresent()) {
                getTextChannel().ifPresent(channel -> channel.removeReactionById(getDrawMessageId().get(), Emojis.X).queue());
            }
            canBeCanceled = false;
        }
    }

    @Override
    public boolean onReaction(GenericGuildMessageReactionEvent event) throws Throwable {
        if (status == Status.ACTIVE) {
            if (canBeCanceled && EmojiUtil.reactionEmoteEqualsEmoji(event.getReactionEmote(), Emojis.X)) {
                canBeCanceled = false;
                cancel(false, true);
                return true;
            } else {
                boolean success = onReactionCasino(event);
                if (success) {
                    disableCanceling();
                }
                return success;
            }
        } else {
            deregisterListeners();

            Command command = CommandManager.createCommandByClass(this.getClass(), getLocale(), getPrefix());
            getGuildMessageReceivedEvent().ifPresent(e -> CommandManager.manage(e, command, String.valueOf(coinsInput), Instant.now()));
            return false;
        }
    }

    public Response onMessageInputCasino(GuildMessageReceivedEvent event, String input) throws Throwable {
        return null;
    }

    @Override
    public Response onMessageInput(GuildMessageReceivedEvent event, String input) throws Throwable {
        Response response = onMessageInputCasino(event, input);
        if (response != null) {
            disableCanceling();
        }
        return response;
    }

    @Override
    public EmbedBuilder draw() {
        EmbedBuilder eb = drawCasino(getMember().map(Member::getEffectiveName).orElse(TextManager.getString(getLocale(), TextManager.GENERAL, "notfound", StringUtil.numToHex(getMemberId().get()))), coinsInput);
        if (eb != null && retryRequestAdded) {
            if (getLog() != null && getLog().length() > 0) {
                EmbedUtil.addLog(eb, getLogStatus(), getLog());
            }

            eb.addField(Emojis.ZERO_WIDTH_SPACE, TextManager.getString(getLocale(), Category.CASINO, "casino_retry", RETRY_EMOJI), false);
            setLog(null, null);
        }
        return eb;
    }

    @Override
    public void onListenerTimeOut() {
        if (status == Status.ACTIVE) {
            cancel(true, false);
            EmbedBuilder eb = draw();
            if (eb != null) {
                drawMessage(eb);
            }
        }
    }

}
