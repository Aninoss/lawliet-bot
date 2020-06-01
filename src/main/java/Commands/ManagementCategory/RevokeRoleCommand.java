package Commands.ManagementCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnReactionAddListener;
import CommandSupporters.Command;
import Constants.Permission;
import Core.DiscordApiCollection;
import Core.EmbedFactory;
import Core.Mention.MentionUtil;
import Core.PermissionCheck;
import Core.TextManager;
import Core.Utils.StringUtil;
import Modules.RoleAssigner;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
