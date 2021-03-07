package commands.runnables.gimmickscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.MemberAccountAbstract;
import core.EmbedFactory;
import net.dv8tion.jda.api.EmbedBuilder;

@CommandProperties(
        trigger = "avatar",
        emoji = "\uD83D\uDDBC️️",
        executableWithoutArgs = true,
        aliases = { "profilepic" }
)
public class AvatarCommand extends MemberAccountAbstract {

    public AvatarCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder generateUserEmbed(Server server, User user, boolean userIsAuthor, String followedString) throws Throwable {
        String avatarUrl = user.getAvatar().getUrl().toString() + "?size=2048";
        return EmbedFactory.getEmbedDefault(this, getString("template",user.getDisplayName(server), avatarUrl)).setImage(avatarUrl);
    }

}
