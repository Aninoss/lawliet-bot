package commands.runnables;

import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.CommandManager;
import commands.listeners.OnButtonListener;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.mention.Mention;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.casinologs.CasinoLogCache;
import modules.casinologs.CasinoLogEntry;
import modules.fishery.FisheryPowerUp;
import modules.fishery.FisheryStatus;
import mysql.modules.casinostats.DBCasinoStats;
import mysql.modules.casinotracking.DBCasinoTracking;
import mysql.modules.gamestatistics.DBGameStatistics;
import mysql.modules.gamestatistics.GameStatisticsData;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

public abstract class CasinoAbstract extends Command implements OnButtonListener {

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
    private EmbedBuilder lastEmbedBuilder;
    private boolean hasCancelButton;
    private boolean trackingActive;
    private CasinoLogEntry casinoLogEntry;

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
        casinoLogEntry = new CasinoLogEntry(getTrigger());
        CasinoLogCache.put(event.getGuild().getIdLong(), event.getMember().getIdLong(), casinoLogEntry);

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
                return true;
            }

            if (getGuildEntity().getFishery().getFisheryStatus() != FisheryStatus.ACTIVE) {
                coinsInput = 0;
                trackingActive = false;
                registerButtonListener(event.getMember());
                return true;
            }

            FisheryMemberData memberBean = FisheryUserManager.getGuildData(event.getGuild().getIdLong()).getMemberData(event.getMember().getIdLong());
            long coins = memberBean.getCoins();
            long value = Math.min(MentionUtil.getAmountExt(args, coins), coins);
            if (value == -1) {
                coinsInput = (long) Math.ceil(coins * 0.1);
                trackingActive = coinsInput > 0 && DBCasinoTracking.getInstance().retrieve().isActive(event.getUser().getIdLong());
                memberBean.addCoinsHidden(coinsInput);
                registerButtonListener(event.getMember());
                return true;
            }

            if (value >= 0) {
                coinsInput = value;
                trackingActive = coinsInput > 0 && DBCasinoTracking.getInstance().retrieve().isActive(event.getUser().getIdLong());
                memberBean.addCoinsHidden(coinsInput);
                registerButtonListener(event.getMember());
                return true;
            } else {
                drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "0")))
                        .exceptionally(ExceptionLogger.get());
                return false;
            }
        } catch (Throwable e) {
            casinoLogEntry.addEvent("Error");
            cancel(event.getMember(), false, true);
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

    protected void endGame(Member member, boolean requestRetry) {
        FisheryUserManager.getGuildData(getGuildId().get()).getMemberData(getMemberId().get()).addCoinsHidden(-coinsInput);
        if (requestRetry) {
            registerButtonListener(member, false);
            setComponents(BUTTON_RETRY);
        } else {
            setActionRows();
        }
    }

    protected void tie(Member member) {
        casinoLogEntry.addEvent("Tie");
        endGame(member, true);
        status = Status.DRAW;
        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), Category.CASINO, "casino_draw"));

        if (trackingActive) {
            DBCasinoStats.getInstance().retrieve(new DBCasinoStats.Key(member.getGuild().getIdLong(), member.getIdLong()))
                    .add(getTrigger(), false, 0);
        }
    }

    protected void lose(Member member, boolean requestRetry) {
        casinoLogEntry.addEvent("Lose");
        endGame(member, requestRetry);
        status = Status.LOST;
        setLog(LogStatus.LOSE, TextManager.getString(getLocale(), Category.CASINO, "casino_lose"));

        if (coinsInput > 0 && useCalculatedMultiplicator) {
            DBGameStatistics.getInstance().retrieve(compareKey).addValue(false, 1);
        }

        long coinsLost = coinsInput;
        if (coinsInput > 0) {
            FisheryMemberData fisheryMemberData = FisheryUserManager.getGuildData(getGuildId().get()).getMemberData(getMemberId().get());
            EmbedBuilder eb;
            if (fisheryMemberData.getActivePowerUps().contains(FisheryPowerUp.SHIELD)) {
                casinoLogEntry.addEvent("Shield Protection");
                fisheryMemberData.deletePowerUp(FisheryPowerUp.SHIELD);
                coinsLost = 0;
                Mention mentionedMembers = MentionUtil.getMentionedStringOfMembers(getLocale(), List.of(member));
                eb = EmbedFactory.getEmbedDefault()
                        .setDescription(TextManager.getString(getLocale(), Category.CASINO, "casino_protection", mentionedMembers.isMultiple(), mentionedMembers.getMentionText()))
                        .setThumbnail("https://cdn.discordapp.com/attachments/1077245845440827562/1080855203026313276/shield_break.gif");
            } else {
                eb = fisheryMemberData.changeValuesEmbed(member, 0, -coinsInput, getGuildEntity());
            }
            setAdditionalEmbeds(eb.build());
        }

        if (trackingActive) {
            DBCasinoStats.getInstance().retrieve(new DBCasinoStats.Key(member.getGuild().getIdLong(), member.getIdLong()))
                    .add(getTrigger(), false, coinsLost);
        }
    }

    protected void cancel(Member member, boolean loseBet, boolean requestRetry) {
        casinoLogEntry.addEvent("Cancel");
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
        casinoLogEntry.addEvent("Win");
        endGame(member, true);
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
        EmbedBuilder eb = FisheryUserManager.getGuildData(member.getGuild().getIdLong()).getMemberData(getMemberId().get())
                .changeValuesEmbed(member, 0, valueWon, getGuildEntity());
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

    @Override
    public EmbedBuilder draw(Member member) {
        lastEmbedBuilder = drawCasino(getMemberEffectiveName().orElse(TextManager.getString(getLocale(), TextManager.GENERAL, "notfound", StringUtil.numToHex(getMemberId().get()))), coinsInput);
        hasCancelButton = getActionRows().stream().anyMatch(b -> b.getButtons().contains(BUTTON_CANCEL));
        return lastEmbedBuilder;
    }

    @Override
    public void onListenerTimeOut() {
        if (status == Status.ACTIVE) {
            casinoLogEntry.addEvent("Time Out");
            getMember().ifPresentOrElse(member -> {
                cancel(member, !hasCancelButton, false);
                EmbedBuilder eb = draw(member);
                if (eb != null) {
                    drawMessage(eb).exceptionally(ExceptionLogger.get());
                }
            }, () -> {
                if (!hasCancelButton) {
                    FisheryMemberData memberData = FisheryUserManager.getGuildData(getGuildId().get()).getMemberData(getMemberId().get());
                    memberData.addCoinsHidden(-coinsInput);
                    memberData.changeValues(0, -coinsInput);

                    if (trackingActive) {
                        DBCasinoStats.getInstance().retrieve(new DBCasinoStats.Key(getGuildId().get(), getMemberId().get()))
                                .add(getTrigger(), false, coinsInput);
                    }
                }
            });
        }
    }

}
