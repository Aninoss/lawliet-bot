package Commands.ManagementCategory;

import CommandListeners.CommandProperties;
import Constants.Permission;

import java.util.Locale;

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

    public RevokeRoleCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean addRole() { return false; }

}
