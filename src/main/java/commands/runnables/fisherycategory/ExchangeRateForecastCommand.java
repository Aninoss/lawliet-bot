package commands.runnables.fisherycategory;

import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import modules.ExchangeRate;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "exchf",
        emoji = "🔮",
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
        try {
            event.getMessage().getUserAuthor().get().sendMessage(getEmbed()).get();
        } catch (InterruptedException | ExecutionException e) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("failed"), TextManager.getString(getLocale(), TextManager.GENERAL, "error"))).get();
            return false;
        }

        event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("success"))).get();
        return true;
    }

    private EmbedBuilder getEmbed() throws InvalidKeySpecException, NoSuchAlgorithmException {
        return EmbedFactory.getEmbedDefault(this, getString("template",
                StringUtil.numToString(ExchangeRate.getInstance().get(-1)),
                getChangeEmoji(-1),
                StringUtil.numToString(ExchangeRate.getInstance().get(0)),
                getChangeEmoji(0),
                StringUtil.numToString(ExchangeRate.getInstance().get(1)),
                getChangeEmoji(1),
                StringUtil.numToString(ExchangeRate.getInstance().get(2)),
                getChangeEmoji(2)
        ));
    }

    private String getChangeEmoji(int offset) throws InvalidKeySpecException, NoSuchAlgorithmException {
        int rateNow = ExchangeRate.getInstance().get(offset);
        int rateBefore = ExchangeRate.getInstance().get(offset - 1);

        if (rateNow > rateBefore) return "\uD83D\uDD3A";
        else {
            if (rateNow < rateBefore) return "\uD83D\uDD3B";
            else return "•";
        }
    }

}
