package Commands.External;

import CommandListeners.CommandProperties;

@CommandProperties(
    trigger = "softyuri",
    emoji = "\uD83D\uDC6D",
    withLoadingBar = true,
    executable = true
)
public class SoftYuriCommand extends SafebooruProxyCommand {

    public SoftYuriCommand() {
        super("yuri", false);
    }

}