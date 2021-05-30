package commands.runnables.informationcategory;

import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import constants.ExternalLinks;
import core.EmbedFactory;
import core.buttons.ButtonStyle;
import core.buttons.MessageButton;
import core.buttons.MessageSendActionAdvanced;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "invite",
        emoji = "\uD83D\uDD17",
        executableWithoutArgs = true,
        onlyPublicVersion = true,
        aliases = { "link", "addbot", "inv" }
)
public class InviteCommand extends Command {

    public InviteCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("template"));
        new MessageSendActionAdvanced(event.getChannel())
                .appendButtons(new MessageButton(ButtonStyle.LINK, getString("button"), ExternalLinks.BOT_INVITE_URL))
                .embed(eb.build())
                .queue();
        return true;
    }

}
