package Commands.Emotes;

import CommandSupporters.Command;
import General.EmbedFactory;
import General.Mention.Mention;
import General.TextManager;
import General.Tools;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

public class EmoteCommand extends Command {
    String[] gifs;

    EmoteCommand() {
        super();
        privateUse = false;
        botPermissions = 0;
        userPermissions = 0;
        nsfw = false;
        withLoadingBar = false;
        executable = true;
    }

    public boolean onInteractionRecieved(MessageCreateEvent event, String followedString, ArrayList<Integer> picked) throws Throwable {
        String gifUrl = gifs[Tools.pickFullRandom(picked, gifs.length)];
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this,getString("template", "**"+event.getMessage().getAuthor().getDisplayName()+"**"))
                .setImage(gifUrl);

        event.getMessage().getChannel().sendMessage(eb).get();
        removeMessageForwarder();

        return true;
    }
}
