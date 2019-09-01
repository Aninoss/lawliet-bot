package Commands.External;

import CommandListeners.onRecievedListener;
import Commands.NSFW.PornProxyCommand;

public class SafebooruProxyCommand extends PornProxyCommand implements onRecievedListener {
    public SafebooruProxyCommand(String search, boolean gifOnly) {
        super(search, "", gifOnly, "safebooru.org", "https://safebooru.org/images/%d/%f");
        nsfw = false;
    }
}
