package Commands.InformationCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Constants.Settings;
import Core.EmbedFactory;
import org.javacord.api.event.message.MessageCreateEvent;

@CommandProperties(
        trigger = "invite",
        emoji = "\uD83D\uDD17",
        executable = true,
        aliases = {"link", "addbot", "inv"}
)
public class InviteCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("template", Settings.BOT_INVITE_URL))).get();
        return true;
    }
    
}
