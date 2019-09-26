package Commands.Moderation;

import CommandListeners.CommandProperties;
import Constants.Permission;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.server.Server;

@CommandProperties(
    trigger = "kick",
    botPermissions = Permission.KICK_USER,
    userPermissions = Permission.KICK_USER,
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
        server.kickUser(user).get();
    }

    @Override
    public boolean canProcess(Server server, User userStarter, User userAim) {
        return server.canYouKickUser(userAim) && server.canBanUser(userStarter, userAim) && !server.isOwner(userAim);
    }
}
