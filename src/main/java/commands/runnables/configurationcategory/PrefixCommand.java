package commands.runnables.configurationcategory;

import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ShardManager;
import core.TextManager;
import mysql.modules.server.DBServer;

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
                DBServer.getInstance().retrieve(event.getServer().get().getId()).setPrefix(followedString);

                if (server.canYouChangeOwnNickname()) {
                    String nickname = server.getDisplayName(ShardManager.getInstance().getSelf()).trim();
                    String[] nicknameArray = nickname.split("\\[");

                    if (nicknameArray.length == 1) {
                        server.updateNickname(ShardManager.getInstance().getSelf(), nickname + " [" + followedString + "]");
                    } else if (nicknameArray.length == 2 && nicknameArray[1].contains("]")) {
                        server.updateNickname(ShardManager.getInstance().getSelf(), nicknameArray[0].trim() + " [" + followedString + "]");
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
