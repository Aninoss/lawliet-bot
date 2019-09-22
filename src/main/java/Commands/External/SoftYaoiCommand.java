package Commands.External;

import CommandListeners.CommandProperties;

@CommandProperties(
    trigger = "softyaoi",
    emoji = "\uD83D\uDC6C",
    withLoadingBar = true,
    executable = true
)
public class SoftYaoiCommand extends SafebooruProxyCommand {

    public SoftYaoiCommand() {
        super("yaoi", false);
    }

}