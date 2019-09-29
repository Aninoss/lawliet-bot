package Commands.External;

import CommandListeners.CommandProperties;

@CommandProperties(
        trigger = "softyuri",
        emoji = "\uD83D\uDC6D",
        withLoadingBar = true,
        executable = true,
        aliases = {"safeyuri"}
)
public class SoftYuriCommand extends SafebooruProxyCommand {

    public SoftYuriCommand() {
        super("yuri", false);
    }

}