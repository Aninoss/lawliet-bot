package commands.runnables.managementcategory;

import commands.listeners.CommandProperties;
import constants.Permission;

import java.util.Locale;

@CommandProperties(
        trigger = "revokerole",
        userPermissions = Permission.MANAGE_ROLES,
        botPermissions = Permission.MANAGE_ROLES,
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
