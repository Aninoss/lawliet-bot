package commands.runnables.fisherycategory;

import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.TextManager;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import modules.ExchangeRate;
import modules.Fishery;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "exchf",
        emoji = "🔮",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        executableWithoutArgs = true,
        patreonRequired = true,
        aliases = { "exchforecast", "exchangerateforecast", "erforecast", "exchrforecast", "exchangeforecast" }
)
public class ExchangeRateForecastCommand extends Command {

    public ExchangeRateForecastCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        JDAUtil.sendPrivateMessage(event.getMember(), getEmbed().build())
                .queue(message -> {
                    event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("success")).build())
                            .queue();
                }, e -> {
                    event.getChannel()
                            .sendMessage(EmbedFactory.getEmbedError(
                                    this,
                                    getString("failed"),
                                    TextManager.getString(getLocale(), TextManager.GENERAL, "error")
                            ).build())
                            .queue();
                });

        return true;
    }

    private EmbedBuilder getEmbed() {
        return EmbedFactory.getEmbedDefault(this, getString(
                "template",
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

}
