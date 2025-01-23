package commands.runnables.fisherycategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.listeners.OnButtonListener;
import commands.listeners.OnMessageInputListener;
import commands.runnables.FisheryInterface;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.modals.ModalMediator;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.fishery.ExchangeRate;
import modules.fishery.Fishery;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@CommandProperties(
        trigger = "sell",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83D\uDCE4",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = {"s"}
)
public class SellCommand extends Command implements FisheryInterface, OnButtonListener, OnMessageInputListener {

    private static final String BUTTON_ID_ENTERNUMBER = "enter_number";
    private static final String BUTTON_ID_SELLALL = "sell_all";
    private static final String BUTTON_ID_CANCEL = "cancel";

    private FisheryMemberData fisheryMemberData;
    private EmbedBuilder eb;
    private boolean completed = false;

    public SellCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(CommandEvent event, String args) {
        fisheryMemberData = FisheryUserManager.getGuildData(event.getGuild().getIdLong())
                .getMemberData(event.getMember().getIdLong());

        if (!args.isEmpty()) {
            String error = process(event.getMember(), args);
            if (error == null) {
                drawMessage(eb).exceptionally(ExceptionLogger.get());
                return true;
            } else {
                setLog(LogStatus.FAILURE, error);
            }
        }

        eb = EmbedFactory.getEmbedDefault(
                this,
                getString(
                        "status",
                        StringUtil.numToString(fisheryMemberData.getFish()),
                        StringUtil.numToString(fisheryMemberData.getCoins()),
                        StringUtil.numToString(ExchangeRate.get(0)),
                        Fishery.getChangeEmoji()
                )
        );

        registerButtonListener(event.getMember());
        registerMessageInputListener(event.getMember(), false);
        return true;
    }

    @Override
    public MessageInputResponse onMessageInput(@NotNull MessageReceivedEvent event, @NotNull String input) throws Throwable {
        String error = process(event.getMember(), input);
        if (error == null) {
            deregisterListenersWithComponents();
            return MessageInputResponse.SUCCESS;
        } else {
            setLog(LogStatus.FAILURE, error);
            return MessageInputResponse.FAILED;
        }
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        switch (event.getComponentId()) {
            case BUTTON_ID_CANCEL -> {
                markNoInterest();
                return true;
            }
            case BUTTON_ID_SELLALL -> {
                String error = process(event.getMember(), "all");
                if (error != null) {
                    setLog(LogStatus.FAILURE, error);
                }
                return true;
            }
            case BUTTON_ID_ENTERNUMBER -> {
                String id = "text";
                TextInput textInput = TextInput.create(id, getString("number"), TextInputStyle.SHORT)
                        .setRequiredRange(1, 50)
                        .build();

                Modal modal = ModalMediator.createDrawableCommandModal(this, getString("enternumber"), e -> {
                            String error = process(event.getMember(), e.getValue(id).getAsString());
                            if (error != null) {
                                setLog(LogStatus.FAILURE, error);
                            }
                            return null;
                        })
                        .addActionRow(textInput)
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
        }
        return true;
    }

    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        if (!completed) {
            setComponents(
                    Button.of(ButtonStyle.PRIMARY, BUTTON_ID_ENTERNUMBER, getString("enternumber")),
                    Button.of(ButtonStyle.PRIMARY, BUTTON_ID_SELLALL, getString("sellall")),
                    Button.of(ButtonStyle.SECONDARY, BUTTON_ID_CANCEL, TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort"))
            );
        }
        return new EmbedBuilder(eb);
    }

    private String process(Member member, String args) {
        long value = Math.min(MentionUtil.getAmountExt(args, fisheryMemberData.getFish()), fisheryMemberData.getFish());

        if (args.equalsIgnoreCase("no")) {
            markNoInterest();
            return null;
        }

        if (value >= 1) {
            long coins = ExchangeRate.get(0) * value;
            eb = EmbedFactory.getEmbedDefault(this, getString("done"));
            setAdditionalEmbeds(fisheryMemberData.changeValuesEmbed(member, -value, coins, getGuildEntity()).build());
            completed = true;
            deregisterListenersWithComponents();
            return null;
        } else if (value == 0) {
            if (fisheryMemberData.getFish() <= 0) {
                return getString("nofish");
            } else {
                return TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1");
            }
        } else if (value == -1) {
            return TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit");
        }

        return null;
    }

    private void markNoInterest() {
        eb = EmbedFactory.getEmbedDefault(this, getString("nointerest_description", StringUtil.numToString(ExchangeRate.get(0)), Fishery.getChangeEmoji()));
        completed = true;
        deregisterListenersWithComponents();
    }

}
