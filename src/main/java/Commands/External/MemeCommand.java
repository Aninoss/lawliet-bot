package Commands.External;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Commands.NSFW.RedditTemplateCommand;
import General.*;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

public class MemeCommand extends RedditTemplateCommand {
    public MemeCommand() {
        super("memes");
        trigger = "meme";
        nsfw = false;
        emoji = "\uD83D\uDDBC";
    }
}
