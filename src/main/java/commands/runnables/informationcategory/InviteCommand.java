package commands.runnables.informationcategory;

import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import constants.ExternalLinks;
import core.EmbedFactory;
import core.components.ActionRows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

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
        event.getChannel().sendMessageEmbeds(eb.build())
                .setActionRows(ActionRows.of(Button.of(ButtonStyle.LINK, ExternalLinks.BOT_INVITE_URL, getString("button"))))
                .queue();
        return true;
    }

}
