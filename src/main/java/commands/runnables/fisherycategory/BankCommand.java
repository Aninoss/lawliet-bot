package commands.runnables.fisherycategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.LogStatus;
import constants.Settings;
import core.EmbedFactory;
import core.ListGen;
import core.modals.ModalMediator;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import mysql.redis.fisheryusers.FisheryMemberBankDeposit;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@CommandProperties(
        trigger = "bank",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "🏦",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        releaseDate = { 2025, 10, 7 }
)
public class BankCommand extends NavigationAbstract implements FisheryInterface {

    public static final int STATE_CONFIRM = 1;

    private long coinsToDeposit;

    public BankCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(CommandEvent event, String args) throws Throwable {
        registerNavigationListener(event.getMember());
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonMain(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                String id = "text";
                TextInput textInput = TextInput.create(id, getString("main_add_deposit_label"), TextInputStyle.SHORT)
                        .setRequiredRange(1, 50)
                        .build();

                Modal modal = ModalMediator.createDrawableCommandModal(this, getString("main_add_deposit_title"), e -> {
                            String input = e.getValue(id).getAsString();
                            FisheryMemberData fisheryMemberData = FisheryUserManager.getGuildData(e.getGuild().getIdLong()).getMemberData(e.getMember().getIdLong());
                            long coins = MentionUtil.getAmountExt(input, fisheryMemberData.getCoins());
                            if (coins < 10) {
                                setLog(LogStatus.FAILURE, getString("main_add_deposit_error_invalid_value"));
                                return null;
                            }

                            coinsToDeposit = coins;
                            setState(STATE_CONFIRM);
                            return null;
                        })
                        .addActionRow(textInput)
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case 1 -> {
                FisheryMemberData fisheryMemberData = FisheryUserManager.getGuildData(event.getGuild().getIdLong()).getMemberData(event.getMember().getIdLong());
                List<FisheryMemberBankDeposit> deposits = fisheryMemberData.getBankDeposits();
                ArrayList<FisheryMemberBankDeposit> newDeposits = new ArrayList<>();

                long coinsAdd = 0;
                int numberOfClaimedDeposits = 0;
                for (FisheryMemberBankDeposit deposit : deposits) {
                    if (Instant.now().isBefore(deposit.getUntil())) {
                        newDeposits.add(deposit);
                        continue;
                    }

                    coinsAdd += getDepositValueAfterDuration(deposit.getCoins());
                    numberOfClaimedDeposits++;
                }

                if (numberOfClaimedDeposits > 0) {
                    fisheryMemberData.setBankDeposits(newDeposits);
                    fisheryMemberData.addCoinsRaw(coinsAdd);
                    setLog(LogStatus.SUCCESS, getString("main_claim_success", numberOfClaimedDeposits != 1, StringUtil.numToString(numberOfClaimedDeposits)));
                } else {
                    setLog(LogStatus.FAILURE, getString("main_claim_error"));
                }
                return true;
            }
        }
        return false;
    }

    @ControllerButton(state = STATE_CONFIRM)
    public boolean onButtonConfirm(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
        } else {
            FisheryMemberData fisheryMemberData = FisheryUserManager.getGuildData(event.getGuild().getIdLong()).getMemberData(event.getMember().getIdLong());
            if (fisheryMemberData.getCoins() < coinsToDeposit) {
                setLog(LogStatus.FAILURE, getString("confirm_error_not_enough"));
                setState(DEFAULT_STATE);
                return true;
            }

            fisheryMemberData.addCoinsRaw(-coinsToDeposit);
            fisheryMemberData.addBankDeposit(coinsToDeposit, Instant.now().plus(Duration.ofDays(7)));
            setLog(LogStatus.SUCCESS, getString("main_add_deposit_success"));
            setState(DEFAULT_STATE);
        }
        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawMain(Member member) {
        FisheryMemberData fisheryMemberData = FisheryUserManager.getGuildData(member.getGuild().getIdLong()).getMemberData(member.getIdLong());
        String list = new ListGen<FisheryMemberBankDeposit>().getList(fisheryMemberData.getBankDeposits(), getLocale(), ListGen.SLOT_TYPE_BULLET, deposit ->
                getString("main_deposits_slot", Instant.now().isAfter(deposit.getUntil()), StringUtil.numToString(deposit.getCoins()), StringUtil.numToString(getDepositValueAfterDuration(deposit.getCoins())), TimeFormat.RELATIVE.atInstant(deposit.getUntil()).toString())
        );

        setComponents(getString("main_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("main_desc", StringUtil.numToString(fisheryMemberData.getCoins())))
                .addField(getString("main_deposits_title"), list, false);
    }

    @Draw(state = STATE_CONFIRM)
    public EmbedBuilder onDrawConfirm(Member member) {
        String field = getString(
                "confirm_field",
                StringUtil.numToString(coinsToDeposit),
                StringUtil.numToString(getDepositValueAfterDuration(coinsToDeposit)),
                TimeFormat.DATE_TIME_SHORT.atInstant(Instant.now().plus(Duration.ofDays(7))).toString()
        );

        FisheryMemberData fisheryMemberData = FisheryUserManager.getGuildData(member.getGuild().getIdLong()).getMemberData(member.getIdLong());
        setComponents(getString("confirm_options").split("\n"), Set.of(0));
        return EmbedFactory.getEmbedDefault(this, getString("confirm_desc", StringUtil.numToString(fisheryMemberData.getCoins())), getString("confirm_title"))
                .addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), field, false);
    }

    private long getDepositValueAfterDuration(long coins) {
        return Math.min(Settings.FISHERY_MAX, (long) (coins * 1.1));
    }

}
