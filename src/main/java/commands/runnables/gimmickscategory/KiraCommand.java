package commands.runnables.gimmickscategory;

import commands.listeners.CommandProperties;
import commands.runnables.UserAccountAbstract;
import core.EmbedFactory;

import java.util.Locale;
import java.util.Random;

@CommandProperties(
    trigger = "kira",
    emoji = "\u270D\uFE0F️️",
    executableWithoutArgs = true
)
public class KiraCommand extends UserAccountAbstract {

    public KiraCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder generateUserEmbed(Server server, User user, boolean userIsAuthor, String followedString) throws Throwable {
        Random r = new Random(user.hashCode());
        int percent = r.nextInt(101);
        return EmbedFactory.getEmbedDefault(this, getString("template",user.getDisplayName(server), String.valueOf(percent)))
                .setThumbnail("http://images4.fanpop.com/image/photos/18000000/Kira-death-note-18041689-200-200.jpg");
    }

}
