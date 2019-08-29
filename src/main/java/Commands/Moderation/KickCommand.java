package Commands.Moderation;

import Constants.Permission;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.server.Server;

public class KickCommand extends WarnCommand  {
    public KickCommand() {
        super();
        trigger = "kick";
        privateUse = false;
        botPermissions = Permission.KICK_USER;
        userPermissions = Permission.KICK_USER;
        nsfw = false;
        withLoadingBar = false;
        thumbnail = "http://icons.iconarchive.com/icons/elegantthemes/beautiful-flat/128/door-icon.png";
        emoji = "\uD83D\uDEAA";
        executable = false;
    }

    @Override
    public void process(Server server, User user) throws Throwable {
        server.kickUser(user).get();
    }

    @Override
    public boolean canProcess(Server server, User userStarter, User userAim) {
        return server.canYouKickUser(userAim) && server.canBanUser(userStarter, userAim);
    }
}
