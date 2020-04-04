package Commands.BotOwnerCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import General.EmbedFactory;
import General.Mention.MentionTools;
import General.Mention.MentionList;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
    trigger = "send",
    privateUse = true,
    emoji = "\uD83D\uDDE8️",
    executable = false
)
public class SendCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        String[] split = followedString.split(" ");
        if (split.length < 2) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this));
        } else {
            MentionList<User> userMarked = MentionTools.getUsers(event.getMessage() ,followedString);
            ArrayList<User> list = userMarked.getList();
            String content = userMarked.getResultMessageString();

            for(User user: list) {
                user.sendMessage(EmbedFactory.getEmbed()
                        .setAuthor(event.getMessage().getUserAuthor().get())
                        .setDescription(content)).get();
            }

            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("text"))).get();
            return true;
        }
        return false;
    }

}
