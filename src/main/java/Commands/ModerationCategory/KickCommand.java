package Commands.ModerationCategory;

import CommandListeners.CommandProperties;
import Constants.Permission;
import General.PermissionCheck;
import General.StringTools;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.server.Server;

@CommandProperties(
    trigger = "kick",
    botPermissions = Permission.KICK_MEMBERS,
    userPermissions = Permission.KICK_MEMBERS | Permission.BAN_MEMBERS,
    thumbnail = "http://icons.iconarchive.com/icons/elegantthemes/beautiful-flat/128/door-icon.png",
    emoji = "\uD83D\uDEAA",
    executable = false
)
public class KickCommand extends WarnCommand  {

    @Override
    public void process(Server server, User user) throws Throwable {
        server.kickUser(user, reason).get();
    }

    @Override
    public boolean canProcess(Server server, User userStarter, User userAim) {
        return PermissionCheck.canYouKickUser(server, userAim) && server.canKickUser(userStarter, userAim);
    }

}
