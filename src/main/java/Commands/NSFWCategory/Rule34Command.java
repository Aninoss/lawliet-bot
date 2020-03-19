package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import Commands.PornAbstract;
import General.*;
import MySQL.DBServer;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "r34",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        withLoadingBar = true,
        aliases = {"rule34", "34"}
)
public class Rule34Command extends PornAbstract {

    @Override
    protected String getDomain() {
        return "rule34.xxx";
    }

    @Override
    protected String getImageTemplate() {
        return "https://img.rule34.xxx/images/%d/%f";
    }

}
