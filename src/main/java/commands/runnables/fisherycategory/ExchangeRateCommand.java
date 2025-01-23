package commands.runnables.fisherycategory;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import commands.listeners.OnButtonListener;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.cache.PatreonCache;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import modules.fishery.ExchangeRate;
import modules.fishery.Fishery;
import modules.schedulers.AlertResponse;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "exch",
        emoji = "\uD83D\uDD01",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "exchangerate", "er", "exchr", "exchange", "exchf", "exchforecast", "exchangerateforecast", "erforecast", "exchrforecast", "exchangeforecast" }
)
public class ExchangeRateCommand extends Command implements OnButtonListener, OnAlertListener {

    private String textInclude = null;

    public ExchangeRateCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        if (PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), false)) {
            setComponents(Button.of(ButtonStyle.PRIMARY, "forecast", getString("forecast_button")));
            registerButtonListener(event.getMember());
        } else {
            textInclude = getString("forecast_patreon");
            drawMessage(generateEmbed(false)).exceptionally(ExceptionLogger.get());
        }
        return true;
    }

    private EmbedBuilder generateEmbed(boolean alert) {
        StringBuilder sb = new StringBuilder(getString(alert ? "template_alert" : "template", StringUtil.numToString(ExchangeRate.get(0)), Fishery.getChangeEmoji()));
        if (textInclude != null) {
            sb.append("\n")
                    .append(textInclude);
            textInclude = null;
        }

        return EmbedFactory.getEmbedDefault(this, sb.toString());
    }

    private EmbedBuilder generateUserEmbed() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append(
                    getString("forecast_slot", true,
                            getString("forecast_day", i),
                            StringUtil.numToString(ExchangeRate.get(i - 1)),
                            Fishery.getChangeEmoji(i - 1)
                    )
            ).append("\n");
        }

        return EmbedFactory.getEmbedDefault(this, sb.toString());
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        if (getInteractionResponse().isValid()) {
            getInteractionResponse().replyEmbeds(List.of(generateUserEmbed().build()), true)
                    .queue();
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(Member member) {
        EmbedBuilder eb = generateEmbed(false);
        EmbedUtil.addTrackerNoteLog(getLocale(), member, eb, getPrefix(), getTrigger());
        return eb;
    }

    @Override
    public @NotNull AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        slot.sendMessage(getLocale(), true, generateEmbed(true).build());
        slot.setNextRequest(TimeUtil.setInstantToNextDay(Instant.now()).plusSeconds(10));

        return AlertResponse.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

}
