package commands.runnables.gimmickscategory;

import commands.listeners.CommandProperties;
import commands.runnables.UserAccountAbstract;
import core.EmbedFactory;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import java.util.Locale;

@CommandProperties(
        trigger = "avatar",
        emoji = "\uD83D\uDDBC️️",
        executableWithoutArgs = true,
        aliases = { "profilepic" }
)
public class AvatarCommand extends UserAccountAbstract {

    public AvatarCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder generateUserEmbed(Server server, User user, boolean userIsAuthor, String followedString) throws Throwable {
        String avatarUrl = user.getAvatar().getUrl().toString() + "?size=2048";
        return EmbedFactory.getEmbedDefault(this, getString("template",user.getDisplayName(server), avatarUrl)).setImage(avatarUrl);
    }

}
