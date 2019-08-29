package Commands.External;

import CommandListeners.onRecievedListener;
import Commands.NSFW.PornCommand;
import org.javacord.api.event.message.MessageCreateEvent;

public class SafebooruCommand extends PornCommand implements onRecievedListener {
    public SafebooruCommand() {
        super();
        trigger = "safeb";
        nsfw = false;
        domain = "safebooru.org";
        imageTemplate = "https://safebooru.org/images/%d/%f";
        emoji = "\uD83D\uDE07";
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onPornRequestRecieved(event, followedString, "");
    }
}
