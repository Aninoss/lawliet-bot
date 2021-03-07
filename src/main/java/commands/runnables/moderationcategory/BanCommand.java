package commands.runnables.moderationcategory;

import commands.listeners.CommandProperties;
import core.MainLogger;
import core.utils.BotPermissionUtil;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
    trigger = "ban",
    botPermissions = PermissionDeprecated.BAN_MEMBERS,
    userPermissions = PermissionDeprecated.BAN_MEMBERS,
    emoji = "\uD83D\uDEAB",
    executableWithoutArgs = false
)
public class BanCommand extends WarnCommand  {

    public BanCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public void process(Server server, User user) throws Throwable {
        try {
            server.banUser(user, 1, reason).get();
        } catch (InterruptedException | ExecutionException e) {
            MainLogger.get().error("Exception on ban", e);
            server.banUser(user).get();
        }
    }

    @Override
    protected boolean autoActions() {
        return false;
    }

    @Override
    public boolean canProcess(Server server, User userStarter, User userAim) {
        return BotPermissionUtil.canBan(server, userAim) && server.canBanUser(userStarter, userAim);
    }
    
}
