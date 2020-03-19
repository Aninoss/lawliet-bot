package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import Commands.PornAbstract;
import General.Tools;
import MySQL.DBServer;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "realb",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        withLoadingBar = true,
        aliases = {"rbooru", "realbooru"}
)
public class RealbooruCommand extends PornAbstract {

    @Override
    protected String getDomain() {
        return "realbooru.com";
    }

    @Override
    protected String getImageTemplate() {
        return "https://realbooru.com/images/%d/%f";
    }

}
