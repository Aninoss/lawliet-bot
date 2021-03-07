package commands.runnables.fisherycategory;

import commands.listeners.CommandProperties;
import commands.runnables.FisheryUserAccountAbstract;
import mysql.modules.fisheryusers.DBFishery;
import net.dv8tion.jda.api.EmbedBuilder;
import java.util.Locale;

@CommandProperties(
        trigger = "acc",
        botPermissions = PermissionDeprecated.USE_EXTERNAL_EMOJIS,
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
        return DBFishery.getInstance().retrieve(server.getId()).getUserBean(user.getId()).getAccountEmbed();
    }

}
