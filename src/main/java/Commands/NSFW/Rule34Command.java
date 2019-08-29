package Commands.NSFW;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.*;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

public class Rule34Command extends PornCommand implements onRecievedListener {
    public Rule34Command() {
        super();
        trigger = "r34";
        domain = "rule34.xxx";
        imageTemplate = "https://img.rule34.xxx/images/%d/%f";
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onPornRequestRecieved(event, followedString, " -loli -shota");
    }
}
