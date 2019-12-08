package Commands.NSFW;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Settings;
import General.*;
import org.javacord.api.event.message.MessageCreateEvent;

public class GelbooruProxyCommand extends PornProxyCommand implements onRecievedListener {

    public GelbooruProxyCommand(String search, boolean gifOnly) {
        super(search, Tools.getNSFWTagRemoveList() + " -japanese_(nationality)", gifOnly, "gelbooru.com", "http://simg3.gelbooru.com/samples/%d/sample_%f");
    }

}
