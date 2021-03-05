package commands.runnables.moderationcategory;

import commands.listeners.CommandProperties;
import constants.PermissionDeprecated;
import core.utils.BotPermissionUtil;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
    trigger = "kick",
    botPermissions = PermissionDeprecated.KICK_MEMBERS,
    userPermissions = PermissionDeprecated.KICK_MEMBERS,
    emoji = "\uD83D\uDEAA",
    executableWithoutArgs = false
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
            MainLogger.get().error("Exception on kick", e);
            server.kickUser(user).get();
        }
    }

    @Override
    protected boolean autoActions() {
        return false;
    }

    @Override
    public boolean canProcess(Server server, User userStarter, User userAim) {
        return BotPermissionUtil.canKick(server, userAim) && server.canKickUser(userStarter, userAim);
    }

}
