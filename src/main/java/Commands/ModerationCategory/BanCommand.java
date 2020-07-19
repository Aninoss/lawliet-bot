package Commands.ModerationCategory;

import CommandListeners.CommandProperties;
import Constants.Permission;
import Core.Utils.PermissionUtil;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

@CommandProperties(
    trigger = "ban",
    botPermissions = Permission.BAN_MEMBERS,
    userPermissions = Permission.KICK_MEMBERS | Permission.BAN_MEMBERS,
    emoji = "\uD83D\uDEAB",
    executable = false
)
public class BanCommand extends WarnCommand  {

    private final static Logger LOGGER = LoggerFactory.getLogger(BanCommand.class);

    @Override
    public void process(Server server, User user) throws Throwable {
        try {
            server.banUser(user, 1, reason).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Exception on ban", e);
            server.banUser(user).get();
        }
    }

    @Override
    public boolean canProcess(Server server, User userStarter, User userAim) {
        return PermissionUtil.canYouBanUser(server, userAim) && server.canBanUser(userStarter, userAim);
    }
    
}
