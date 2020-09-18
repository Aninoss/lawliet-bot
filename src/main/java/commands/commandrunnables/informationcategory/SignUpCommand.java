package commands.commandrunnables.informationcategory;

import commands.commandlisteners.CommandProperties;
import commands.Command;
import constants.Settings;
import core.EmbedFactory;
import core.utils.StringUtil;
import mysql.DBGiveaway;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Locale;

@CommandProperties(
        trigger = "signup",
        emoji = "✏️",
        executable = true,
        aliases = {"giveaway", "singup", "register"}
)
public class SignUpCommand extends Command {

    public SignUpCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        boolean success = DBGiveaway.registerGiveaway(event.getServer().get(), event.getMessage().getUserAuthor().get());

        if (success) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this,
                    getString("success", Settings.SERVER_INVITE_URL,
                            event.getMessage().getUserAuthor().get().getMentionTag(),
                            StringUtil.escapeMarkdown(event.getServer().get().getName())
                    )
            )).get();
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    getString("exists", Settings.SERVER_INVITE_URL),
                    getString("exists_title")
            )).get();
        }

        return success;
    }
    
}
