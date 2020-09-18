package commands.commandslots.moderationcategory;

import commands.commandlisteners.CommandProperties;
import constants.Permission;
import core.utils.PermissionUtil;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
    trigger = "kick",
    botPermissions = Permission.KICK_MEMBERS,
    userPermissions = Permission.KICK_MEMBERS,
    emoji = "\uD83D\uDEAA",
    executable = false
)
public class KickCommand extends WarnCommand  {

    private final static Logger LOGGER = LoggerFactory.getLogger(KickCommand.class);

    public KickCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public void process(Server server, User user) throws Throwable {
        try {
            server.kickUser(user, reason).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Exception on kick", e);
            server.kickUser(user).get();
        }
    }

    @Override
    protected boolean autoMod() {
        return false;
    }

    @Override
    public boolean canProcess(Server server, User userStarter, User userAim) {
        return PermissionUtil.canYouKickUser(server, userAim) && server.canKickUser(userStarter, userAim);
    }

}
