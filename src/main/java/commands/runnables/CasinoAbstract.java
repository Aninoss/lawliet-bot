package commands.runnables;

import java.time.Instant;
import java.util.Locale;
import commands.Command;
import commands.CommandManager;
import commands.listeners.OnButtonListener;
import commands.listeners.OnMessageInputListener;
import constants.Category;
import constants.FisheryStatus;
import constants.LogStatus;
import constants.Response;
import core.EmbedFactory;
import core.TextManager;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import mysql.modules.gamestatistics.DBGameStatistics;
import mysql.modules.gamestatistics.GameStatisticsData;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

public abstract class CasinoAbstract extends Command implements OnButtonListener, OnMessageInputListener {

    public enum Status { ACTIVE, WON, LOST, DRAW, CANCELED }

    public static final String BUTTON_ID_RETRY = "retry";
    public static final String BUTTON_ID_QUIT = "quit";

    protected final Button BUTTON_RETRY = Button.of(ButtonStyle.PRIMARY,  BUTTON_ID_RETRY, TextManager.getString(getLocale(), Category.CASINO, "casino_retry"));
    protected final Button BUTTON_CANCEL = Button.of(ButtonStyle.SECONDARY, BUTTON_ID_QUIT, TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort"));

    private static final double BONUS_MULTIPLICATOR = 1;

    private String compareKey;
    private long coinsInput;
    private double winMultiplicator = 1;
    private Status status = Status.ACTIVE;
    private final boolean useCalculatedMultiplicator;
    private final boolean allowBet;
    private boolean canBeCanceled = true;
    private boolean retryRequestAdded = false;

    public CasinoAbstract(Locale locale, String prefix, boolean allowBet, boolean useCalculatedMultiplicator) {
        super(locale, prefix);
        this.compareKey = getTrigger();
        this.allowBet = allowBet;
        this.useCalculatedMultiplicator = useCalculatedMultiplicator;
    }

    public abstract boolean onGameStart(GuildMessageReceivedEvent event, String args) throws Throwable;

    public abstract boolean onButtonCasino(ButtonClickEvent event) throws Throwable;

    public abstract EmbedBuilder drawCasino(String playerName, long coinsInput);

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws Throwable {
        try {
            if (!allowBet) {
                setLog(LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "nobet"));
            }
            if (!onGameStart(event, args)) {
                return false;
            }

            if (!allowBet) {
                coinsInput = 0;
                registerButtonListener();
                registerMessageInputListener(false);
                return true;
            }

            FisheryStatus status = DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).getFisheryStatus();
            if (status != FisheryStatus.ACTIVE) {
                coinsInput = 0;
                registerButtonListener();
                registerMessageInputListener(false);
                return true;
            }

            FisheryMemberData memberBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberBean(event.getMember().getIdLong());
            long coins = memberBean.getCoins();
            long value = Math.min(MentionUtil.getAmountExt(args, coins), coins);
            if (value == -1) {
                coinsInput = (long) Math.ceil(coins * 0.1);
                memberBean.addHiddenCoins(coinsInput);
                registerButtonListener();
                registerMessageInputListener(false);
                return true;
            }

            if (value >= 0) {
                coinsInput = value;
                memberBean.addHiddenCoins(coinsInput);
                registerButtonListener();
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
            if (requestRetry && !retryRequestAdded) {
                registerButtonListener();
                setButtons(BUTTON_RETRY);
                retryRequestAdded = true;
            } else {
                setButtons();
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
                GameStatisticsData gameStatisticsData = DBGameStatistics.getInstance().retrieve(compareKey);
                gameStatisticsData.addValue(true, winMultiplicator);

                double won = gameStatisticsData.getValue(true);
                double lost = gameStatisticsData.getValue(false);
                if (won > 0 && lost > 0) multiplicator = lost / won;
            }

            EmbedBuilder eb = DBFishery.getInstance().retrieve(guild.getIdLong()).getMemberBean(getMemberId().get())
                    .changeValuesEmbed(0, (long) Math.ceil(coinsWon * multiplicator * BONUS_MULTIPLICATOR));
            if (coinsInput > 0) {
                getTextChannel().ifPresent(channel -> channel.sendMessage(eb.build()).queue());
            }
        });
    }

    @Override
    public boolean onButton(ButtonClickEvent event) throws Throwable {
        if (status == Status.ACTIVE) {
            if (hasCancelButton() && event.getComponentId().equals(BUTTON_ID_QUIT)) {
                canBeCanceled = false;
                cancel(false, true);
                return true;
            } else {
                return onButtonCasino(event);
            }
        } else if (event.getComponentId().equals(BUTTON_ID_RETRY)) {
            deregisterListeners();
            redrawMessageWithoutButtons();
            Command command = CommandManager.createCommandByClass(this.getClass(), getLocale(), getPrefix());
            getGuildMessageReceivedEvent().ifPresent(e -> CommandManager.manage(e, command, String.valueOf(coinsInput), Instant.now()));
            return false;
        }
        return false;
    }

    private boolean hasCancelButton() {
        return getActionRows().stream().anyMatch(b -> b.getButtons().contains(BUTTON_CANCEL));
    }

    public Response onMessageInputCasino(GuildMessageReceivedEvent event, String input) throws Throwable {
        return null;
    }

    @Override
    public Response onMessageInput(GuildMessageReceivedEvent event, String input) throws Throwable {
        if (status == Status.ACTIVE) {
            return onMessageInputCasino(event, input);
        }
        return null;
    }

    @Override
    public EmbedBuilder draw() {
        return drawCasino(getMember().map(Member::getEffectiveName).orElse(TextManager.getString(getLocale(), TextManager.GENERAL, "notfound", StringUtil.numToHex(getMemberId().get()))), coinsInput);
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
