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
import java.util.Locale;

@CommandProperties(
        trigger = "avatar",
        emoji = "\uD83D\uDDBC️️",
        executable = true,
        aliases = { "profilepic" }
)
public class AvatarCommand extends Command {

    public AvatarCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

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
        for (User user: list) {
            String avatarUrl = user.getAvatar().getUrl().toString() + "?size=2048";
            EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this,
                    getString("template",user.getDisplayName(server), avatarUrl))
                    .setImage(avatarUrl);

            if (!userMentioned) {
                eb.setFooter(TextManager.getString(getLocale(), TextManager.GENERAL, "mention_optional"));
                if (followedString.length() > 0)
                    EmbedFactory.addNoResultsLog(eb, getLocale(), followedString);
            }

            event.getChannel().sendMessage(eb).get();
        }

        return true;
    }

}
