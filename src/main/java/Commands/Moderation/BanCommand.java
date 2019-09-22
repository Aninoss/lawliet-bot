package Commands.Moderation;

import CommandListeners.CommandProperties;
import Constants.Permission;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

@CommandProperties(
    trigger = "ban",
    botPermissions = Permission.BAN_USER,
    userPermissions = Permission.BAN_USER,
    thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat/128/close-2-icon.png",
    emoji = "\uD83D\uDEAB",
    executable = false
)
public class BanCommand extends WarnCommand  {

    public BanCommand() {
        super();
    }

    @Override
    public void process(Server server, User user) throws Throwable {
        server.banUser(user).get();
    }

    @Override
    public boolean canProcess(Server server, User userStarter, User userAim) {
        return server.canYouBanUser(userAim) && server.canBanUser(userStarter, userAim);
    }
    
}
