package Commands.Information;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Settings;
import General.EmbedFactory;
import org.javacord.api.event.message.MessageCreateEvent;

@CommandProperties(
    trigger = "upvote",
    thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/128/thumbs-up-icon.png",
    emoji = "\uD83D\uDC4D",
    executable = true
)
public class UpvoteCommand extends Command implements onRecievedListener {

    public UpvoteCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("template", Settings.UPVOTE_URL))).get();
        return true;
    }

}