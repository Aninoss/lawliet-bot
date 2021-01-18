package commands.runnables.fisherycategory;

import commands.listeners.CommandProperties;
import commands.runnables.FisheryUserAccountAbstract;
import constants.Permission;
import mysql.modules.fisheryusers.DBFishery;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import java.util.Locale;

@CommandProperties(
        trigger = "acc",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        emoji = "\uD83D\uDE4B",
        executableWithoutArgs = true,
        aliases = { "profile", "profil", "account", "balance", "bal", "a" }
)
public class AccountCommand extends FisheryUserAccountAbstract {

    public AccountCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder generateUserEmbed(Server server, User user, boolean userIsAuthor, String followedString) throws Throwable {
        return DBFishery.getInstance().getBean(server.getId()).getUserBean(user.getId()).getAccountEmbed();
    }

}
