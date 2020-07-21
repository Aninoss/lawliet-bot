package Commands.InteractionsCategory;

import CommandListeners.CommandProperties;
import CommandSupporters.Command;
import Core.EmbedFactory;
import Core.Mention.MentionList;
import Core.Mention.MentionUtil;
import Core.RandomPicker;
import Core.TextManager;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.List;

@CommandProperties(
    trigger = "nibble",
    emoji = "👂",
    exlusiveUsers = {397209883793162240L, 710120672499728426L},
    executable = true
)
public class NibbleCommand extends Command {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/60369861b53a2f4c2b0e1012220d63fd/tenor.gif?itemid=14714300",
                "https://cdn.weeb.sh/images/rkakblmiZ.gif",
                "https://media1.tenor.com/images/110de9f955f52fcce475013ed978210d/tenor.gif?itemid=15638962"
        };
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Server server = event.getServer().get();
        User user0 = event.getMessage().getUserAuthor().get();

        MentionList<User> userMention = MentionUtil.getUsers(event.getMessage(), followedString);
        List<User> userList = userMention.getList();
        if (userList.isEmpty()) {
            EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL,"no_mentions"));
            event.getChannel().sendMessage(eb).get();
            return false;
        }
        User user1 = userList.get(0);

        if (user0.getId() != 397209883793162240L && user1.getId() != 397209883793162240L) {
            EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this, getString("wrong_user"));
            event.getChannel().sendMessage(eb).get();
            return false;
        }

        String text = userMention.getResultMessageString();
        if (text.isEmpty()) text = getString("default");

        String[] gifs = getGifs();
        String gifUrl = gifs[RandomPicker.getInstance().pick(getTrigger(), event.getServer().get().getId(), gifs.length)];

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("template", user0.getDisplayName(server), user1.getDisplayName(server), text))
                .setImage(gifUrl);
        event.getChannel().sendMessage(eb).get();

        return true;
    }

}
