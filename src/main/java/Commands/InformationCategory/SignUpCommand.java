package Commands.InformationCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Constants.Settings;
import General.EmbedFactory;
import MySQL.DBUser;
import org.javacord.api.event.message.MessageCreateEvent;

@CommandProperties(
        trigger = "signup",
        thumbnail = "http://icons.iconarchive.com/icons/custom-icon-design/flatastic-3/128/signup-icon.png",
        emoji = "✏️",
        executable = true,
        aliases = {"giveaway"}
)
public class SignUpCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        boolean success = DBUser.registerGiveaway(event.getServer().get(), event.getMessage().getUserAuthor().get());

        if (success) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedSuccess(this,
                    getString("success", Settings.SERVER_INVITE_URL,
                            event.getMessage().getUserAuthor().get().getMentionTag(),
                            event.getServer().get().getName()
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
