package Commands.NSFW;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Settings;
import General.*;
import MySQL.DBServer;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "gelb",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        withLoadingBar = true,
        aliases = {"gel"}
)
public class GelbooruCommand extends PornCommand implements onRecievedListener {

    public GelbooruCommand() {
        super();
        domain = "gelbooru.com";
        imageTemplate = "https://simg3.gelbooru.com/samples/%d/sample_%f";
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        ArrayList<String> nsfwFilter = DBServer.getNSFWFilterFromServer(event.getServer().get());
        return onPornRequestRecieved(event, followedString, Tools.getNSFWTagRemoveList(nsfwFilter), nsfwFilter);
    }

}
