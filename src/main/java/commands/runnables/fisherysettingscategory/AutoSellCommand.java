package commands.runnables.fisherysettingscategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import constants.Settings;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.cache.PatreonCache;
import core.modals.ModalMediator;
import core.utils.StringUtil;
import mysql.modules.autosell.DBAutoSell;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@CommandProperties(
        trigger = "autosell",
        emoji = "🤖",
        usesExtEmotes = true,
        executableWithoutArgs = true
)
public class AutoSellCommand extends Command implements OnButtonListener {

    private enum Mode { PENDING, SET, ERROR }

    private Mode mode = Mode.PENDING;

    public AutoSellCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        if (args.length() > 0) {
            Integer newThreshold = null;
            boolean valueFound = false;

            if (args.equalsIgnoreCase("off")) {
                valueFound = true;
            } else if (StringUtil.stringIsInt(args)) {
                newThreshold = Math.min(255, Integer.parseInt(args));
                valueFound = newThreshold >= 0;
            }

            if (!valueFound) {
                String invalid = TextManager.getString(getLocale(), TextManager.GENERAL, "invalid", args);
                drawMessageNew(EmbedFactory.getEmbedError(this, invalid))
                        .exceptionally(ExceptionLogger.get());
                return false;
            }

            EmbedBuilder eb;
            if (setThreshold(event.getMember(), newThreshold)) {
                eb = generateSetEmbed(newThreshold);
            } else {
                eb = generateErrorEmbed();
            }
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        } else {
            setComponents(
                    Button.of(ButtonStyle.SUCCESS, "true", TextManager.getString(getLocale(), TextManager.GENERAL, "function_button", 1)),
                    Button.of(ButtonStyle.DANGER, "false", TextManager.getString(getLocale(), TextManager.GENERAL, "function_button", 0))
            );
            registerButtonListener(event.getMember());
        }
        return true;
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        boolean active = Boolean.parseBoolean(event.getComponentId());
        if (active) {
            if (!PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), false)) {
                deregisterListenersWithComponents();
                mode = Mode.ERROR;
                return true;
            }

            Integer currentThreshold = DBAutoSell.getInstance().retrieve().getThreshold(event.getMember().getIdLong());
            TextInput textInput = TextInput.create("threshold", getString("modal_textinput"), TextInputStyle.SHORT)
                    .setValue(currentThreshold != null ? String.valueOf(currentThreshold) : null)
                    .setMinLength(1)
                    .setMaxLength(3)
                    .build();

            Modal modal = ModalMediator.createDrawableCommandModal(this, getString("modal_title"), e -> {
                        deregisterListeners();
                        String newThresholdString = e.getValues().get(0).getAsString();
                        int newThreshold = StringUtil.stringIsInt(newThresholdString)
                                ? Math.min(255, Integer.parseInt(newThresholdString))
                                : -1;

                        if (newThreshold < 0) {
                            String invalid = TextManager.getString(getLocale(), TextManager.GENERAL, "invalid", newThresholdString);
                            return EmbedFactory.getEmbedError(this, invalid);
                        }

                        if (setThreshold(event.getMember(), newThreshold)) {
                            mode = Mode.SET;
                        } else {
                            mode = Mode.ERROR;
                        }
                        return null;
                    })
                    .addActionRow(textInput)
                    .build();
            event.replyModal(modal).queue();
            return false;
        } else {
            deregisterListenersWithComponents();
            setThreshold(event.getMember(), null);
            mode = Mode.SET;
            return true;
        }
    }

    @Nullable
    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        Integer currentThreshold = DBAutoSell.getInstance().retrieve().getThreshold(member.getIdLong());
        switch (mode) {
            case SET -> {
                return generateSetEmbed(currentThreshold);
            }
            case ERROR -> {
                return generateErrorEmbed();
            }
            default -> {
                String thresholdValueText = currentThreshold == null
                        ? StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), false)
                        : getString("threshold_fish", StringUtil.numToString(currentThreshold));
                String thresholdText = getString("threshold", thresholdValueText);
                return EmbedFactory.getEmbedDefault(this, getCommandLanguage().getDescLong() + thresholdText);
            }
        }
    }

    private boolean setThreshold(Member member, Integer threshold) {
        if (threshold == null || PatreonCache.getInstance().hasPremium(member.getIdLong(), false)) {
            DBAutoSell.getInstance().retrieve().setThreshold(member.getIdLong(), threshold);
            if (threshold != null) {
                FisheryUserManager.getGuildData(member.getGuild().getIdLong())
                        .getMemberData(member.getIdLong())
                        .processAutoSell();
            }
            return true;
        } else {
            return false;
        }
    }

    private EmbedBuilder generateSetEmbed(Integer threshold) {
        String text = threshold != null
                ? getString("set_value", StringUtil.numToString(threshold), getUsername().get())
                : TextManager.getString(getLocale(), TextManager.GENERAL, "function_onoff_member", false, getCommandLanguage().getTitle(), getUsername().get());
        return EmbedFactory.getEmbedDefault(this, text);
    }

    private EmbedBuilder generateErrorEmbed() {
        setComponents(EmbedFactory.getPatreonBlockButtons(getLocale()));
        return EmbedFactory.getEmbedDefault(this, getString("error"))
                .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_title"))
                .setColor(Settings.PREMIUM_COLOR);
    }

}
