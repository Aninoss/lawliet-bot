package Commands.ModerationCategory;

import CommandListeners.CommandProperties;
import Constants.Permission;
import General.PermissionCheck;
import General.StringTools;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

@CommandProperties(
    trigger = "ban",
    botPermissions = Permission.BAN_MEMBERS,
    userPermissions = Permission.KICK_MEMBERS | Permission.BAN_MEMBERS,
    thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat/128/close-2-icon.png",
    emoji = "\uD83D\uDEAB",
    executable = false
)
public class BanCommand extends WarnCommand  {

    @Override
    public void process(Server server, User user) throws Throwable {
        server.banUser(user, 7, reason).get();
    }

    @Override
    public boolean canProcess(Server server, User userStarter, User userAim) {
        return PermissionCheck.canYouBanUser(server, userAim) && server.canBanUser(userStarter, userAim);
    }
    
}
