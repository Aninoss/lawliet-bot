package commands.runnables.configurationcategory;

import commands.Command;
import commands.listeners.CommandProperties;
import constants.PermissionDeprecated;
import core.EmbedFactory;
import core.DiscordApiManager;
import core.TextManager;
import core.utils.StringUtil;
import mysql.modules.server.DBServer;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Locale;

@CommandProperties(
    trigger = "prefix",
    userPermissions = PermissionDeprecated.MANAGE_SERVER,
    emoji = "\uD83D\uDCDB",
    executableWithoutArgs = false
)
public class PrefixCommand extends Command {

    public PrefixCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Server server = event.getServer().get();
        if (followedString.length() > 0) {
            if (followedString.length() <= 5) {
                DBServer.getInstance().getBean(event.getServer().get().getId()).setPrefix(followedString);

                if (server.canYouChangeOwnNickname()) {
                    String nickname = StringUtil.trimString(server.getDisplayName(DiscordApiManager.getInstance().getSelf()));
                    String[] nicknameArray = nickname.split("\\[");

                    if (nicknameArray.length == 1) {
                        server.updateNickname(DiscordApiManager.getInstance().getSelf(), nickname + " [" + followedString + "]");
                    } else if (nicknameArray.length == 2 && nicknameArray[1].contains("]")) {
                        server.updateNickname(DiscordApiManager.getInstance().getSelf(), StringUtil.trimString(nicknameArray[0]) + " [" + followedString + "]");
                    }
                }

                event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("changed", followedString))).get();
                return true;
            } else {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                        TextManager.getString(getLocale(), TextManager.GENERAL, "args_too_long", "5"))).get();
                return false;
            }
        } else {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    getString("no_arg"))).get();
            return false;
        }
    }
}
