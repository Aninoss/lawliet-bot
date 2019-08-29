package Commands.BotManagement;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Settings;
import General.EmbedFactory;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

public class UpvoteCommand extends Command implements onRecievedListener {
    public UpvoteCommand() {
        super();
        trigger = "upvote";
        privateUse = false;
        botPermissions = 0;
        userPermissions = 0;
        nsfw = false;
        withLoadingBar = false;
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/128/thumbs-up-icon.png";
        emoji = "\uD83D\uDC4D";
        executable = true;
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("template", Settings.UPVOTE_URL))).get();
        return true;
    }
}
