package Commands.ManagementCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Permission;
import General.DiscordApiCollection;
import General.EmbedFactory;
import General.TextManager;
import General.StringTools;
import MySQL.Server.DBServer;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;

@CommandProperties(
    trigger = "prefix",
    userPermissions = Permission.MANAGE_SERVER,
    thumbnail = "http://icons.iconarchive.com/icons/graphicloads/flat-finance/128/name-card-icon.png",
    emoji = "\uD83D\uDCDB",
    executable = false
)
public class PrefixCommand extends Command implements onRecievedListener {

    public PrefixCommand() {
        super();
    }

    @Override
    public boolean onReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Server server = event.getServer().get();
        if (followedString.length() > 0) {
            if (followedString.length() <= 5) {
                DBServer.getInstance().getBean(event.getServer().get().getId()).setPrefix(followedString);

                if (server.canYouChangeOwnNickname()) {
                    String nickname = StringTools.trimString(server.getDisplayName(DiscordApiCollection.getInstance().getYourself()));
                    String[] nicknameArray = nickname.split("\\[");

                    if (nicknameArray.length == 1) {
                        server.updateNickname(DiscordApiCollection.getInstance().getYourself(), nickname + " [" + followedString + "]");
                    } else if (nicknameArray.length == 2 && nicknameArray[1].contains("]")) {
                        server.updateNickname(DiscordApiCollection.getInstance().getYourself(), StringTools.trimString(nicknameArray[0]) + " [" + followedString + "]");
                    }
                }

                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedSuccess(this, getString("changed", followedString))).get();
                return true;
            } else {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                        TextManager.getString(getLocale(), TextManager.GENERAL, "args_too_long", "5"))).get();
                return false;
            }
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    getString("no_arg"))).get();
            return false;
        }
    }
}
