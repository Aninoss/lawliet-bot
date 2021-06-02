package commands.runnables.fisherycategory;

import java.time.Instant;
import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import commands.listeners.OnButtonListener;
import constants.TrackerResult;
import core.EmbedFactory;
import core.cache.PatreonCache;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import modules.ExchangeRate;
import modules.Fishery;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

@CommandProperties(
        trigger = "exch",
        emoji = "\uD83D\uDD01",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        executableWithoutArgs = true,
        aliases = { "exchangerate", "er", "exchr", "exchange", "exchf", "exchforecast", "exchangerateforecast", "erforecast", "exchrforecast", "exchangeforecast" }
)
public class ExchangeRateCommand extends Command implements OnButtonListener, OnAlertListener {

    private String textInclude = null;

    public ExchangeRateCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        if (PatreonCache.getInstance().getUserTier(event.getMember().getIdLong(), false) >= 2) {
            setButtons(Button.of(ButtonStyle.PRIMARY, "forecast", getString("forecast_button")));
            registerButtonListener();
        } else {
            textInclude = getString("forecast_patreon");
            drawMessage(generateEmbed(false));
        }
        return true;
    }

    private EmbedBuilder generateEmbed(boolean alert) {
        StringBuilder sb = new StringBuilder(getString(alert ? "template_alert" : "template", StringUtil.numToString(ExchangeRate.getInstance().get(0)), Fishery.getChangeEmoji()));
        if (textInclude != null) {
            sb.append("\n")
                    .append(textInclude);
            textInclude = null;
        }

        return EmbedFactory.getEmbedDefault(this, sb.toString());
    }

    private EmbedBuilder generateUserEmbed() {
        return EmbedFactory.getEmbedDefault(this, getString(
                "forecast_template",
                StringUtil.numToString(ExchangeRate.getInstance().get(-1)),
                Fishery.getChangeEmoji(-1),
                StringUtil.numToString(ExchangeRate.getInstance().get(0)),
                Fishery.getChangeEmoji(0),
                StringUtil.numToString(ExchangeRate.getInstance().get(1)),
                Fishery.getChangeEmoji(1),
                StringUtil.numToString(ExchangeRate.getInstance().get(2)),
                Fishery.getChangeEmoji(2)
        ));
    }

    @Override
    public boolean onButton(ButtonClickEvent event) throws Throwable {
        deregisterListenersWithButtons();
        event.getHook().sendMessageEmbeds(generateUserEmbed().build())
                .setEphemeral(true)
                .queue();
        return true;
    }

    @Override
    public EmbedBuilder draw() {
        EmbedBuilder eb = generateEmbed(false);
        getMember().ifPresent(member -> EmbedUtil.addTrackerNoteLog(getLocale(), member, eb, getPrefix(), getTrigger()));
        return eb;
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerData slot) throws Throwable {
        long messageId = slot.sendMessage(true, generateEmbed(true).build()).get();
        slot.setMessageId(messageId);
        slot.setNextRequest(TimeUtil.setInstantToNextDay(Instant.now()).plusSeconds(10));

        return TrackerResult.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

}
