package Commands.ExternalCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import Commands.PornAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "safeb",
        emoji = "\uD83D\uDE07",
        withLoadingBar = true,
        executable = true,
        aliases = {"softb", "safebooru", "softbooru", "sbooru"}
)
public class SafebooruCommand extends PornAbstract {

    @Override
    protected String getDomain() {
        return "safebooru.org";
    }

    @Override
    protected String getImageTemplate() {
        return "https://safebooru.org/images/%d/%f";
    }

}
