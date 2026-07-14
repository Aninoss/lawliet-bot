package commands.runnables.moderationcategory;

import commands.listeners.CommandProperties;
import mysql.hibernate.entity.BotLogEntity;
import net.dv8tion.jda.api.Permission;

import java.util.Locale;

@CommandProperties(
        trigger = "unjail",
        botGuildPermissions = Permission.MANAGE_ROLES,
        userGuildPermissions = Permission.MANAGE_ROLES,
        emoji = "🔒",
        executableWithoutArgs = false,
        requiresFullMemberCache = true,
        aliases = { "dejail", "unisolate", "deisolate" }
)
public class UnjailCommand extends JailCommand {

    public UnjailCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false);
    }

    @Override
    protected BotLogEntity.Event getBotLogEvent() {
        return BotLogEntity.Event.MOD_UNJAIL;
    }

}
