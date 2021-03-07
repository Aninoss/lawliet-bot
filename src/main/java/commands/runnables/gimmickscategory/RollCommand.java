package commands.runnables.gimmickscategory;

import java.util.Locale;
import java.util.Random;
import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;

@CommandProperties(
        trigger = "roll",
        emoji = "\uD83C\uDFB2",
        executableWithoutArgs = true,
        aliases = {"dice", "diceroll"}
)
public class RollCommand extends Command {

    public RollCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Random n = new Random();
        double drawn, border;
        boolean userMentioned = true;

        if (followedString.length() == 0 || !StringUtil.stringIsDouble(followedString)){
            border = 6;
            userMentioned = false;
        }
        else {
            border = Double.parseDouble(followedString);
            if (border < 2) {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL,"too_small", "2"))).get();
                return false;
            }
            if (border > 999999999999999999.0) {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL,"too_large", "999999999999999999"))).get();
                return false;
            }
        }

        drawn = Math.floor(n.nextDouble()*border)+1;

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this,
                getString("result", StringUtil.escapeMarkdown(event.getMessage().getAuthor().getDisplayName()), String.valueOf((long) drawn),String.valueOf((long) border)));
        if (!userMentioned) EmbedUtil.setFooter(eb, this, getString("noarg"));
        event.getChannel().sendMessage(eb).get();
        return true;
    }
}
