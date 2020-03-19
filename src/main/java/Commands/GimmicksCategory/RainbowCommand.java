package Commands.GimmicksCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Permission;
import General.*;
import General.Mention.MentionFinder;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "rainbow",
        botPermissions = Permission.ATTACH_FILES_TO_TEXT_CHANNEL,
        withLoadingBar = true,
        emoji = "\uD83C\uDF08",
        executable = true,
        aliases = {"lgbt"}
)
public class RainbowCommand extends Command implements onRecievedListener {

    public RainbowCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        Server server = event.getServer().get();
        Message message = event.getMessage();
        ArrayList<User> list = MentionFinder.getUsers(message,followedString).getList();
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
            EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this,getString("template",user.getDisplayName(server)))
                    .setImage(ImageCreator.createImageRainbow(user));
            if (!userMentioned) eb.setFooter(TextManager.getString(getLocale(),TextManager.GENERAL,"mention_optional"));
            Message message1 = event.getChannel().sendMessage(eb).get();

            if (message1 != null) {
                String url = message1.getEmbeds().get(0).getImage().get().getUrl().toString();
                eb = EmbedFactory.getEmbed().setDescription(getString("template2", url));
                event.getChannel().sendMessage(eb).get();
            }
        }
        return true;
    }
}
