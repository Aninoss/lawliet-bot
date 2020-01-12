package Commands.Information;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Settings;
import General.EmbedFactory;
import org.javacord.api.event.message.MessageCreateEvent;

@CommandProperties(
        trigger = "invite",
        thumbnail = "http://icons.iconarchive.com/icons/thehoth/seo/128/seo-chain-link-icon.png",
        emoji = "\uD83D\uDD17",
        executable = true,
        aliases = {"link", "addbot"}
)
public class InviteCommand extends Command implements onRecievedListener {

    public InviteCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("template", Settings.BOT_INVITE_URL))).get();
        return true;
    }
    
}
