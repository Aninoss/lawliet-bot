package commands.runnables.utilitycategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import net.dv8tion.jda.api.Permission;

@CommandProperties(
        trigger = "revokerole",
        userGuildPermissions = Permission.MANAGE_ROLES,
        botGuildPermissions = Permission.MANAGE_ROLES,
        emoji = "\uD83D\uDCE4",
        executableWithoutArgs = false,
        patreonRequired = true,
        turnOffTimeout = true,
        aliases = { "takerole", "revoke" }
)
public class RevokeRoleCommand extends AssignRoleCommand {

    public RevokeRoleCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean addRole() { return false; }

}
