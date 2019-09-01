package Commands.NSFW;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.*;
import org.javacord.api.event.message.MessageCreateEvent;

public class RealbooruProxyCommand extends PornProxyCommand implements onRecievedListener {
    public RealbooruProxyCommand(String search, boolean gifOnly) {
        super(search, " -shota -loli", gifOnly, "realbooru.com", "https://realbooru.com/images/%d/%f");
    }
}
