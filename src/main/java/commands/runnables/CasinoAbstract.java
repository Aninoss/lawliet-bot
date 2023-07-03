package commands.runnables;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.CommandManager;
import commands.listeners.MessageInputResponse;
import commands.listeners.OnButtonListener;
import commands.listeners.OnMessageInputListener;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.mention.Mention;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.fishery.FisheryPowerUp;
import modules.fishery.FisheryStatus;
import mysql.modules.casinostats.DBCasinoStats;
import mysql.modules.casinotracking.DBCasinoTracking;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import mysql.modules.gamestatistics.DBGameStatistics;
import mysql.modules.gamestatistics.GameStatisticsData;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

public abstract class CasinoAbstract extends Command implements OnButtonListener, OnMessageInputListener {

    public enum Status { ACTIVE, WON, LOST, DRAW, CANCELED }

    public static final String BUTTON_ID_RETRY = "retry";
    public static final String BUTTON_ID_QUIT = "quit";

    protected final Button BUTTON_RETRY = Button.of(ButtonStyle.PRIMARY, BUTTON_ID_RETRY, TextManager.getString(getLocale(), Category.CASINO, "casino_retry"));
    protected final Button BUTTON_CANCEL = Button.of(ButtonStyle.SECONDARY, BUTTON_ID_QUIT, TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort"));

    private static final double BONUS_MULTIPLICATOR = 1;

    private String compareKey;
    private long coinsInput;
    private double winMultiplicator = 1;
    private Status status = Status.ACTIVE;
    private final boolean useCalculatedMultiplicator;
    private final boolean allowBet;
    private boolean retryRequestAdded = false;
    private EmbedBuilder lastEmbedBuilder;
    private boolean hasCancelButton;
    private boolean trackingActive;

    public CasinoAbstract(Locale locale, String prefix, boolean allowBet, boolean useCalculatedMultiplicator) {
        super(locale, prefix);
        this.compareKey = getTrigger();
        this.allowBet = allowBet;
        this.useCalculatedMultiplicator = useCalculatedMultiplicator;
    }

    public abstract boolean onGameStart(CommandEvent event, String args) throws Throwable;

    public abstract boolean onButtonCasino(ButtonInteractionEvent event) throws Throwable;

    public abstract EmbedBuilder drawCasino(String playerName, long coinsInput);

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        try {
            if (!allowBet) {
                setLog(LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "nobet"));
            }
            if (!onGameStart(event, args)) {
                return false;
            }

            if (!allowBet) {
                coinsInput = 0;
                trackingActive = false;
                registerButtonListener(event.getMember());
                registerMessageInputListener(event.getMember(), false);
                return true;
            }

            FisheryStatus status = DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).getFisheryStatus();
            if (status != FisheryStatus.ACTIVE) {
                coinsInput = 0;
                trackingActive = false;
                registerButtonListener(event.getMember());
                registerMessageInputListener(event.getMember(), false);
                return true;
            }

            FisheryMemberData memberBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberData(event.getMember().getIdLong());
            long coins = memberBean.getCoins();
            long value = Math.min(MentionUtil.getAmountExt(args, coins), coins);
            if (value == -1) {
                coinsInput = (long) Math.ceil(coins * 0.1);
                trackingActive = coinsInput > 0 && DBCasinoTracking.getInstance().retrieve().isActive(event.getUser().getIdLong());
                memberBean.addCoinsHidden(coinsInput);
                registerButtonListener(event.getMember());
                registerMessageInputListener(event.getMember(), false);
                return true;
            }

            if (value >= 0) {
                coinsInput = value;
                trackingActive = coinsInput > 0 && DBCasinoTracking.getInstance().retrieve().isActive(event.getUser().getIdLong());
                memberBean.addCoinsHidden(coinsInput);
                registerButtonListener(event.getMember());
                registerMessageInputListener(event.getMember(), false);
                return true;
            } else {
                drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "0")))
                        .exceptionally(ExceptionLogger.get());
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

    public boolean allowBet() {
        return allowBet;
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

    protected void tie(Member member) {
        endGame(member);
        if (trackingActive) {
            DBCasinoStats.getInstance().retrieve(new DBCasinoStats.Key(member.getGuild().getIdLong(), member.getIdLong()))
                    .add(getTrigger(), false, 0);
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

        long coinsLost = coinsInput;
        if (coinsInput > 0) {
            FisheryMemberData fisheryMemberData = DBFishery.getInstance().retrieve(getGuildId().get()).getMemberData(getMemberId().get());
            EmbedBuilder eb;
            if (fisheryMemberData.getActivePowerUps().contains(FisheryPowerUp.SHIELD)) {
                fisheryMemberData.deletePowerUp(FisheryPowerUp.SHIELD);
                coinsLost = 0;
                Mention mentionedMembers = MentionUtil.getMentionedStringOfMembers(getLocale(), List.of(member));
                eb = EmbedFactory.getEmbedDefault()
                        .setDescription(TextManager.getString(getLocale(), Category.CASINO, "casino_protection", mentionedMembers.isMultiple(), mentionedMembers.getMentionText()))
                        .setThumbnail("https://cdn.discordapp.com/attachments/1077245845440827562/1080855203026313276/shield_break.gif");
            } else {
                eb = fisheryMemberData.changeValuesEmbed(member, 0, -coinsInput, getLocale());
            }
            setAdditionalEmbeds(eb.build());
        }

        if (trackingActive) {
            DBCasinoStats.getInstance().retrieve(new DBCasinoStats.Key(member.getGuild().getIdLong(), member.getIdLong()))
                    .add(getTrigger(), false, coinsLost);
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

        long valueWon = (long) Math.ceil(coinsWon * multiplicator * BONUS_MULTIPLICATOR);
        EmbedBuilder eb = DBFishery.getInstance().retrieve(member.getGuild().getIdLong()).getMemberData(getMemberId().get())
                .changeValuesEmbed(member, 0, valueWon, getLocale());
        if (coinsInput > 0) {
            setAdditionalEmbeds(eb.build());
        }

        if (trackingActive) {
            DBCasinoStats.getInstance().retrieve(new DBCasinoStats.Key(member.getGuild().getIdLong(), member.getIdLong()))
                    .add(getTrigger(), true, valueWon);
        }
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        if (status == Status.ACTIVE) {
            if (hasCancelButton && event.getComponentId().equals(BUTTON_ID_QUIT)) {
                cancel(event.getMember(), false, true);
                return true;
            } else {
                return onButtonCasino(event);
            }
        } else if (event.getComponentId().equals(BUTTON_ID_RETRY)) {
            deregisterListeners();
            setActionRows();
            drawMessage(lastEmbedBuilder).exceptionally(ExceptionLogger.get());
            Command command = CommandManager.createCommandByClass(this.getClass(), getLocale(), getPrefix());
            CommandManager.manage(getCommandEvent(), command, String.valueOf(coinsInput), getGuildEntity(), Instant.now(), false);
            return false;
        }
        return false;
    }

    public MessageInputResponse onMessageInputCasino(MessageReceivedEvent event, String input) throws Throwable {
        return null;
    }

    @Override
    public MessageInputResponse onMessageInput(@NotNull MessageReceivedEvent event, @NotNull String input) throws Throwable {
        if (status == Status.ACTIVE) {
            return onMessageInputCasino(event, input);
        }
        return null;
    }

    @Override
    public EmbedBuilder draw(@NotNull Member member) {
        lastEmbedBuilder = drawCasino(getMemberEffectiveName().orElse(TextManager.getString(getLocale(), TextManager.GENERAL, "notfound", StringUtil.numToHex(getMemberId().get()))), coinsInput);
        hasCancelButton = getActionRows().stream().anyMatch(b -> b.getButtons().contains(BUTTON_CANCEL));
        return lastEmbedBuilder;
    }

    @Override
    public void onListenerTimeOut() {
        if (status == Status.ACTIVE) {
            getMember().ifPresentOrElse(member -> {
                cancel(member, true, false);
                EmbedBuilder eb = draw(member);
                if (eb != null) {
                    drawMessage(eb).exceptionally(ExceptionLogger.get());
                }
            }, () -> {
                FisheryMemberData memberData = DBFishery.getInstance().retrieve(getGuildId().get()).getMemberData(getMemberId().get());
                memberData.addCoinsHidden(-coinsInput);
                memberData.changeValues(0, -coinsInput);

                if (trackingActive) {
                    DBCasinoStats.getInstance().retrieve(new DBCasinoStats.Key(getGuildId().get(), getMemberId().get()))
                            .add(getTrigger(), false, coinsInput);
                }
            });
        }
    }

}
