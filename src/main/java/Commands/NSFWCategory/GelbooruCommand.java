package Commands.NSFWCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import Commands.PornAbstract;
import General.*;
import MySQL.DBServer;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "gelb",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        withLoadingBar = true,
        aliases = {"gel", "gbooru", "gelbooru"}
)
public class GelbooruCommand extends PornAbstract {

    @Override
    protected String getDomain() {
        return "gelbooru.com";
    }

    @Override
    protected String getImageTemplate() {
        return "https://simg3.gelbooru.com/samples/%d/sample_%f";
    }

}
