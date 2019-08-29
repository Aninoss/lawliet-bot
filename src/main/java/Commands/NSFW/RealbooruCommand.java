package Commands.NSFW;

import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

public class RealbooruCommand extends PornCommand implements onRecievedListener {
    public RealbooruCommand() {
        super();
        trigger = "realb";
        domain = "realbooru.com";
        imageTemplate = "https://realbooru.com/images/%d/%f";
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onPornRequestRecieved(event, followedString, " -loli -shota");
    }
}
