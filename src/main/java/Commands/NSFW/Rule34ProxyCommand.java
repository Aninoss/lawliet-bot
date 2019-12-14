package Commands.NSFW;

import CommandListeners.onRecievedListener;
import Constants.Settings;
import General.Tools;

public class Rule34ProxyCommand extends PornProxyCommand implements onRecievedListener {

    public Rule34ProxyCommand(String search, boolean gifOnly) {
        super(search, gifOnly, "rule34.xxx", "https://img.rule34.xxx/images/%d/%f");
    }

}
