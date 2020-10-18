package commands.runnables.informationcategory;

import commands.listeners.CommandProperties;
import commands.Command;
import constants.ExternalLinks;
import core.EmbedFactory;
import core.utils.StringUtil;
import mysql.DBBotGiveaway;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Locale;

@CommandProperties(
        trigger = "signup",
        emoji = "✏️",
        executableWithoutArgs = true,
        aliases = {"giveaway", "singup", "register"}
)
public class SignUpCommand extends Command {

    public SignUpCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        boolean success = DBBotGiveaway.registerGiveaway(event.getServer().get(), event.getMessage().getUserAuthor().get());

        if (success) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this,
                    getString("success", ExternalLinks.SERVER_INVITE_URL,
                            event.getMessage().getUserAuthor().get().getMentionTag(),
                            StringUtil.escapeMarkdown(event.getServer().get().getName())
                    )
            )).get();
        } else {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    getString("exists", ExternalLinks.SERVER_INVITE_URL),
                    getString("exists_title")
            )).get();
        }

        return success;
    }
    
}
