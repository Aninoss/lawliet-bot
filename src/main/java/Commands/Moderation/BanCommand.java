package Commands.Moderation;

import Constants.Permission;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

public class BanCommand extends WarnCommand  {
    public BanCommand() {
        super();
        trigger = "ban";
        privateUse = false;
        botPermissions = Permission.BAN_USER;
        userPermissions = Permission.BAN_USER;
        nsfw = false;
        withLoadingBar = false;
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat/128/close-2-icon.png";
        emoji = "\uD83D\uDEAB";
        executable = false;
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
