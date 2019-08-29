package Commands.BotOwner;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.EmbedFactory;
import General.Mention.MentionFinder;
import General.Mention.MentionList;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.awt.*;
import java.util.ArrayList;

public class SendCommand extends Command implements onRecievedListener {

    public SendCommand() {
        super();
        trigger = "send";
        privateUse = true;
        nsfw = false;
        withLoadingBar = false;
        emoji = "\uD83D\uDDE8️";
        executable = false;
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        String[] split = followedString.split(" ");
        if (split.length < 2) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this));
        } else {
            MentionList<User> userMarked = MentionFinder.getUsers(event.getMessage() ,followedString);
            ArrayList<User> list = userMarked.getList();
            String content = userMarked.getResultMessageString();

            for(User user: list) {
                user.sendMessage(new EmbedBuilder()
                        .setColor(Color.WHITE)
                        .setAuthor(event.getMessage().getUserAuthor().get())
                        .setDescription(content)).get();
            }

            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("text"))).get();
            return true;
        }
        return false;
    }
}
