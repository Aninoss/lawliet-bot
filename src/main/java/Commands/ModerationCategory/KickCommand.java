package Commands.ModerationCategory;

import CommandListeners.CommandProperties;
import Constants.Permission;
import General.Tools;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.server.Server;

@CommandProperties(
    trigger = "kick",
    botPermissions = Permission.KICK_USER,
    userPermissions = Permission.KICK_USER | Permission.BAN_USER,
    thumbnail = "http://icons.iconarchive.com/icons/elegantthemes/beautiful-flat/128/door-icon.png",
    emoji = "\uD83D\uDEAA",
    executable = false
)
public class KickCommand extends WarnCommand  {

    public KickCommand() {
        super();
    }

    @Override
    public void process(Server server, User user) throws Throwable {
        server.kickUser(user, reason).get();
    }

    @Override
    public boolean canProcess(Server server, User userStarter, User userAim) {
        return Tools.canYouKickUser(server, userAim) && server.canKickUser(userStarter, userAim);
    }
}
