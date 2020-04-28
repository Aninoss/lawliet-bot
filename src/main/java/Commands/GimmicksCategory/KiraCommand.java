package Commands.GimmicksCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Core.EmbedFactory;
import Core.Mention.MentionUtil;
import Core.TextManager;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Random;

@CommandProperties(
    trigger = "kira",
    thumbnail = "http://images4.fanpop.com/image/photos/18000000/Kira-death-note-18041689-200-200.jpg",
    emoji = "\u270D\uFE0F️️",
    executable = true
)
public class KiraCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Server server = event.getServer().get();
        Message message = event.getMessage();
        ArrayList<User> list = MentionUtil.getUsers(message,followedString).getList();
        if (list.size() > 5) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    TextManager.getString(getLocale(),TextManager.GENERAL,"too_many_users"))).get();
            return false;
        }
        boolean userMentioned = true;
        if (list.size() == 0) {
            list.add(message.getUserAuthor().get());
            userMentioned = false;
        }
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this);
        for(User user: list) {
            Random r = new Random(user.hashCode());
            int percent = r.nextInt(101);
            eb.addField(user.getDisplayName(server), getString("template",user.getDisplayName(server), String.valueOf(percent)));
        }
        if (!userMentioned) eb.setFooter(TextManager.getString(getLocale(),TextManager.GENERAL,"mention_optional"));
        event.getChannel().sendMessage(eb).get();
        return true;
    }
}
