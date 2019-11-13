package Commands.NSFW;

import CommandListeners.onRecievedListener;

public class Rule34ProxyCommand extends PornProxyCommand implements onRecievedListener {

    public Rule34ProxyCommand(String search, boolean gifOnly) {
        super(search, " -bestiality -shota -loli", gifOnly, "rule34.xxx", "https://img.rule34.xxx/images/%d/%f");
    }

}
