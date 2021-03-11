package commands.runnables;

import java.time.Instant;
import java.util.Locale;
import commands.Command;
import commands.CommandManager;
import commands.listeners.OnReactionListener;
import constants.Category;
import constants.FisheryStatus;
import constants.LogStatus;
import core.EmbedFactory;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.MentionUtil;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberBean;
import mysql.modules.gamestatistics.DBGameStatistics;
import mysql.modules.gamestatistics.GameStatisticsBean;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

public abstract class CasinoAbstract extends Command implements OnReactionListener {

    public enum Status { ACTIVE, WON, LOST, DRAW, ABORT }

    private final double BONUS_MULTIPLICATOR = 1;
    private final String RETRY_EMOJI = "\uD83D\uDD01";

    private String compareKey;
    private long coinsInput;
    private double winMultiplicator = 1;
    private Status status = Status.ACTIVE;
    private final boolean useCalculatedMultiplicator;
    private final boolean allowBet;

    public CasinoAbstract(Locale locale, String prefix, boolean allowBet, boolean useCalculatedMultiplicator) {
        super(locale, prefix);
        this.compareKey = getTrigger();
        this.allowBet = allowBet;
        this.useCalculatedMultiplicator = useCalculatedMultiplicator;
    }

    public abstract String[] onGameStart(GuildMessageReceivedEvent event, String args) throws Throwable;

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

            if (!allowBet) {
                coinsInput = 0;
                registerReactionListener(emojis);
                return true;
            }

            FisheryStatus status = DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).getFisheryStatus();
            if (status != FisheryStatus.ACTIVE) {
                coinsInput = 0;
                registerReactionListener(emojis);
                return true;
            }

            FisheryMemberBean memberBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberBean(event.getMember().getIdLong());
            long coins = memberBean.getCoins();
            long value = Math.min(MentionUtil.getAmountExt(args, coins), coins);
            if (value == -1) {
                coinsInput = (long) Math.ceil(coins * 0.1);
                memberBean.addHiddenCoins(coinsInput);
                registerReactionListener(emojis);
                return true;
            }

            if (value >= 0) {
                coinsInput = value;
                memberBean.addHiddenCoins(coinsInput);
                registerReactionListener(emojis);
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
        getGuild().ifPresent(guild -> {
            status = Status.DRAW;
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), Category.CASINO, "casino_draw"));
            DBFishery.getInstance().retrieve(guild.getIdLong()).getMemberBean(getMemberId().get()).addHiddenCoins(-coinsInput);
            removeReactionListener()
                    .thenRun(() -> registerReactionListener(RETRY_EMOJI));
        });
    }

    protected void lose() {
        getGuild().ifPresent(guild -> {
            endGame();
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

    protected void abort() {
        lose();
        status = Status.ABORT;
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

    public abstract boolean onReactionCasino(GenericGuildMessageReactionEvent event) throws Throwable;

    @Override
    public boolean onReaction(GenericGuildMessageReactionEvent event) throws Throwable {
        if (status == Status.ACTIVE) {
            return onReactionCasino(event);
        } else {
            removeReactionListenerWithMessage();

            Command command = CommandManager.createCommandByClass(this.getClass(), getLocale(), getPrefix());
            getGuildMessageReceivedEvent().ifPresent(e -> CommandManager.manage(e, command, String.valueOf(coinsInput), Instant.now()));
            return false;
        }
    }

    public abstract EmbedBuilder drawCasino(String playerName, long coinsInput);

    @Override
    public EmbedBuilder draw() {
        EmbedBuilder eb = drawCasino(getMember().map(Member::getEffectiveName).orElse("-"), coinsInput);
        if (status != Status.ACTIVE && eb != null) {
            if (getLog() != null && getLog().length() > 0) {
                EmbedUtil.addLog(eb, getLogStatus(), getLog());
            }

            setLog(null, TextManager.getString(getLocale(), Category.CASINO, "casino_retry", RETRY_EMOJI));
        }
        return eb;
    }

    @Override
    public void onReactionTimeOut() {
        if (status == Status.ACTIVE) {
            abort();
            EmbedBuilder eb = draw();
            if (eb != null) {
                drawMessage(eb);
            }
        }
    }

}
