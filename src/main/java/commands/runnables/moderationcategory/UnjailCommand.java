package commands.runnables.moderationcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import net.dv8tion.jda.api.Permission;

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

}
