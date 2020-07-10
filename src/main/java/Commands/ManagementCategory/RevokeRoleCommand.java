package Commands.ManagementCategory;

import CommandListeners.CommandProperties;
import Constants.Permission;

@CommandProperties(
        trigger = "revokerole",
        userPermissions = Permission.MANAGE_ROLES,
        botPermissions = Permission.MANAGE_ROLES,
        emoji = "\uD83D\uDCE4",
        executable = false,
        patreonRequired = true,
        turnOffTimeout = true,
        aliases = { "takerole" }
)
public class RevokeRoleCommand extends AssignRoleCommand {

    @Override
    protected boolean addRole() { return false; }

}
