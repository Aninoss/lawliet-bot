package commands.runnables;

import java.time.Instant;
import java.util.Locale;
import commands.Command;
import commands.CommandManager;
import commands.listeners.OnButtonListener;
import commands.listeners.OnMessageInputListener;
import commands.Category;
import modules.fishery.FisheryStatus;
import constants.LogStatus;
import commands.listeners.MessageInputResponse;
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
                registerButtonListener(event.getMember());
                registerMessageInputListener(event.getMember(), false);
                return true;
            }

            FisheryStatus status = DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).getFisheryStatus();
            if (status != FisheryStatus.ACTIVE) {
                coinsInput = 0;
                registerButtonListener(event.getMember());
                registerMessageInputListener(event.getMember(), false);
                return true;
            }

            FisheryMemberData memberBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberData(event.getMember().getIdLong());
            long coins = memberBean.getCoins();
            long value = Math.min(MentionUtil.getAmountExt(args, coins), coins);
            if (value == -1) {
                coinsInput = (long) Math.ceil(coins * 0.1);
                memberBean.addCoinsHidden(coinsInput);
                registerButtonListener(event.getMember());
                registerMessageInputListener(event.getMember(), false);
                return true;
            }

            if (value >= 0) {
                coinsInput = value;
                memberBean.addCoinsHidden(coinsInput);
                registerButtonListener(event.getMember());
                registerMessageInputListener(event.getMember(), false);
                return true;
            } else {
                event.getChannel()
                        .sendMessageEmbeds(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "0")).build())
                        .queue();
                return false;
            }
        } catch (Throwable e) {
            endGame(event.getMember());
            throw e;
        }
    }

    public void setCompareKey(String compareKey) {
        this.compareKey = compareKey;
    }

    public Status getStatus() {
        return status;
    }

    protected void endGame(Member member) {
        endGame(member, true);
    }

    protected void endGame(Member member, boolean requestRetry) {
        status = Status.DRAW;
        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), Category.CASINO, "casino_draw"));
        DBFishery.getInstance().retrieve(getGuildId().get()).getMemberData(getMemberId().get()).addCoinsHidden(-coinsInput);
        if (requestRetry && !retryRequestAdded) {
            registerButtonListener(member);
            setComponents(BUTTON_RETRY);
            retryRequestAdded = true;
        } else {
            setActionRows();
        }
    }

    protected void lose(Member member) {
        lose(member, true);
    }

    protected void lose(Member member, boolean requestRetry) {
        endGame(member, requestRetry);
        status = Status.LOST;
        setLog(LogStatus.LOSE, TextManager.getString(getLocale(), Category.CASINO, "casino_lose"));
        if (coinsInput > 0 && useCalculatedMultiplicator) {
            DBGameStatistics.getInstance().retrieve(compareKey).addValue(false, 1);
        }
        EmbedBuilder eb = DBFishery.getInstance().retrieve(getGuildId().get()).getMemberData(getMemberId().get())
                .changeValuesEmbed(member, 0, -coinsInput);
        if (coinsInput > 0) {
            setAdditionalEmbeds(eb.build());
        }
    }

    protected void cancel(Member member, boolean loseBet, boolean requestRetry) {
        if (loseBet) {
            lose(member, requestRetry);
        } else {
            endGame(member, requestRetry);
        }
        status = Status.CANCELED;
        setLog(LogStatus.LOSE, TextManager.getString(getLocale(), Category.CASINO, "casino_abort"));
    }

    protected void win(Member member, double winMultiplicator) {
        this.winMultiplicator = winMultiplicator;
        win(member);
    }

    protected void win(Member member) {
        endGame(member);
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

        EmbedBuilder eb = DBFishery.getInstance().retrieve(member.getGuild().getIdLong()).getMemberData(getMemberId().get())
                .changeValuesEmbed(member, 0, (long) Math.ceil(coinsWon * multiplicator * BONUS_MULTIPLICATOR));
        if (coinsInput > 0) {
            setAdditionalEmbeds(eb.build());
        }
    }

    @Override
    public boolean onButton(ButtonClickEvent event) throws Throwable {
        if (status == Status.ACTIVE) {
            if (hasCancelButton() && event.getComponentId().equals(BUTTON_ID_QUIT)) {
                cancel(event.getMember(), false, true);
                return true;
            } else {
                return onButtonCasino(event);
            }
        } else if (event.getComponentId().equals(BUTTON_ID_RETRY)) {
            deregisterListeners();
            redrawMessageWithoutComponents();
            Command command = CommandManager.createCommandByClass(this.getClass(), getLocale(), getPrefix());
            getGuildMessageReceivedEvent().ifPresent(e -> CommandManager.manage(e, command, String.valueOf(coinsInput), Instant.now()));
            return false;
        }
        return false;
    }

    private boolean hasCancelButton() {
        return getActionRows().stream().anyMatch(b -> b.getButtons().contains(BUTTON_CANCEL));
    }

    public MessageInputResponse onMessageInputCasino(GuildMessageReceivedEvent event, String input) throws Throwable {
        return null;
    }

    @Override
    public MessageInputResponse onMessageInput(GuildMessageReceivedEvent event, String input) throws Throwable {
        if (status == Status.ACTIVE) {
            return onMessageInputCasino(event, input);
        }
        return null;
    }

    @Override
    public EmbedBuilder draw(Member member) {
        return drawCasino(getMemberEffectiveName().orElse(TextManager.getString(getLocale(), TextManager.GENERAL, "notfound", StringUtil.numToHex(getMemberId().get()))), coinsInput);
    }

    @Override
    public void onListenerTimeOut() {
        if (status == Status.ACTIVE) {
            getMember().ifPresentOrElse(member -> {
                cancel(member, true, false);
                EmbedBuilder eb = draw(member);
                if (eb != null) {
                    drawMessage(eb);
                }
            }, () -> {
                FisheryMemberData memberData = DBFishery.getInstance().retrieve(getGuildId().get()).getMemberData(getMemberId().get());
                memberData.addCoinsHidden(-coinsInput);
                memberData.changeValues(0, -coinsInput);
            });
        }
    }

}
