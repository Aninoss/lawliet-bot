package Commands.NSFW;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.*;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

@CommandProperties(
        trigger = "gel",
        executable = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        withLoadingBar = true
)
public class GelbooruCommand extends PornCommand implements onRecievedListener {

    public GelbooruCommand() {
        super();
        domain = "gelbooru.com";
        imageTemplate = "https://simg3.gelbooru.com/samples/%d/sample_%f";
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onPornRequestRecieved(event, followedString, " -loli -shota");
    }

}
