package commands.runnables.utilitycategory;

import java.util.Locale;
import commands.listeners.CommandProperties;

@CommandProperties(
        trigger = "revokerole",
        userPermissions = PermissionDeprecated.MANAGE_ROLES,
        botPermissions = PermissionDeprecated.MANAGE_ROLES,
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
