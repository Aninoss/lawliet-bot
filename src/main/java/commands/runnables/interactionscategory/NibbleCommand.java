package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.RandomPicker;
import core.ShardManager;
import core.TextManager;
import core.utils.MentionUtil;
import net.dv8tion.jda.api.EmbedBuilder;

@CommandProperties(
    trigger = "nibble",
    emoji = "👂",
    exclusiveUsers = { 397209883793162240L, 381156056660967426L },
    executableWithoutArgs = true
)
public class NibbleCommand extends Command {

    public NibbleCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/60369861b53a2f4c2b0e1012220d63fd/tenor.gif?itemid=14714300",
                "https://media1.tenor.com/images/110de9f955f52fcce475013ed978210d/tenor.gif?itemid=15638962",
                "https://cdn.weeb.sh/images/rkakblmiZ.gif",
                "https://media1.tenor.com/images/5fcfef1acfa20cdd836aad39512e8fcc/tenor.gif"
        };
    }

    protected String[] getGifsEar() {
        return new String[]{"https://cdn.weeb.sh/images/rkakblmiZ.gif",
                "https://media1.tenor.com/images/5fcfef1acfa20cdd836aad39512e8fcc/tenor.gif"
        };
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Server server = event.getServer().get();
        User user0 = event.getMessage().getUserAuthor().get();

        MentionList<User> userMention = MentionUtil.getMembers(event.getMessage(), followedString);
        List<User> userList = userMention.getList();
        if (userList.isEmpty()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL,"no_mentions"));
            event.getChannel().sendMessage(eb).get();
            return false;
        }
        User user1 = userList.get(0);

        if (user0.getId() != 397209883793162240L &&
                user1.getId() != 397209883793162240L &&
                ShardManager.getInstance().getOwnerId() != user0.getId()
        ) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("wrong_user"));
            event.getChannel().sendMessage(eb).get();
            return false;
        }

        String text = userMention.getResultMessageString();
        if (text.isEmpty()) text = getString("default");

        boolean chooseEarGif = text.toLowerCase().contains("ohr") || text.toLowerCase().contains("ear");
        String[] gifs = chooseEarGif ? getGifsEar() : getGifs();
        String gifUrl = gifs[RandomPicker.getInstance().pick(getTrigger() + chooseEarGif, event.getServer().get().getId(), gifs.length)];

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("template", user0.getDisplayName(server), user1.getDisplayName(server), text))
                .setImage(gifUrl);
        event.getChannel().sendMessage(eb).get();

        return true;
    }

}
