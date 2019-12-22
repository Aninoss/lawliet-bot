package Commands.NSFW;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Settings;
import General.*;
import org.javacord.api.event.message.MessageCreateEvent;

public class GelbooruProxyCommand extends PornProxyCommand implements onRecievedListener {

    public GelbooruProxyCommand(String search, boolean gifOnly) {
        super(search, " -japanese_(nationality) -asian", gifOnly, "gelbooru.com", "http://simg3.gelbooru.com/samples/%d/sample_%f");
    }

}
