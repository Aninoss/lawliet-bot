package Commands.NSFW;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import Constants.Settings;
import General.Tools;
import MySQL.DBServer;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "realb",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        withLoadingBar = true
)
public class RealbooruCommand extends PornCommand implements onRecievedListener {
    public RealbooruCommand() {
        super();
        domain = "realbooru.com";
        imageTemplate = "https://realbooru.com/images/%d/%f";
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        ArrayList<String> nsfwFilter = DBServer.getNSFWFilterFromServer(event.getServer().get());
        return onPornRequestRecieved(event, followedString, Tools.getNSFWTagRemoveList(nsfwFilter), nsfwFilter);
    }
}
