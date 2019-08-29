package Commands.General;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.EmbedFactory;
import General.Mention.MentionFinder;
import General.TextManager;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

public class AvatarCommand extends Command implements onRecievedListener {

    public AvatarCommand() {
        super();
        trigger = "avatar";
        privateUse = false;
        botPermissions = 0;
        userPermissions = 0;
        nsfw = false;
        withLoadingBar = false;
        emoji = "\uD83D\uDDBC️️";
        executable = true;
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        Server server = event.getServer().get();
        Message message = event.getMessage();
        ArrayList<User> list = MentionFinder.getUsers(message,followedString).getList();
        if (list.size() > 5) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    TextManager.getString(locale,TextManager.GENERAL,"too_many_users"))).get();
            return false;
        }
        boolean userMentioned = true;
        if (list.size() == 0) {
            list.add(message.getUserAuthor().get());
            userMentioned = false;
        }
        for (User user: list) {
            EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this,
                    getString("template",user.getDisplayName(server),user.getAvatar().getUrl().toString()))
                    .setThumbnail(user.getAvatar().getUrl().toString());
            if (!userMentioned) eb.setFooter(TextManager.getString(locale,TextManager.GENERAL,"mention_optional"));
            event.getChannel().sendMessage(eb).get();
        }
        return true;
    }
}
