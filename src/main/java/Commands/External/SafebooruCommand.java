package Commands.External;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import Commands.NSFW.PornCommand;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "safeb",
        emoji = "\uD83D\uDE07",
        withLoadingBar = true,
        executable = true,
        aliases = {"softb"}
)
public class SafebooruCommand extends PornCommand implements onRecievedListener {

    public SafebooruCommand() {
        super();
        domain = "safebooru.org";
        imageTemplate = "https://safebooru.org/images/%d/%f";
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onPornRequestRecieved(event, followedString, "", new ArrayList<>());
    }

}
