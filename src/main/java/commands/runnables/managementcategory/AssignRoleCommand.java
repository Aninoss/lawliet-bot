package commands.runnables.managementcategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnReactionAddListener;
import commands.Command;
import constants.Permission;
import core.DiscordApiCollection;
import core.EmbedFactory;
import core.utils.MentionUtil;
import core.utils.PermissionUtil;
import core.TextManager;
import core.utils.StringUtil;
import modules.RoleAssigner;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "assignrole",
        userPermissions = Permission.MANAGE_ROLES,
        botPermissions = Permission.MANAGE_ROLES,
        emoji = "\uD83D\uDCE5",
        executable = false,
        patreonRequired = true,
        turnOffTimeout = true,
        aliases = { "giverole", "assign" }
)
public class AssignRoleCommand extends Command implements OnReactionAddListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(AssignRoleCommand.class);

    private static final String CANCEL_EMOJI = "❌";

    private CompletableFuture<Boolean> future = null;
    private Message message;
    private Role role;

    public AssignRoleCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        ArrayList<Role> roles = MentionUtil.getRoles(event.getMessage(), followedString).getList();

        /* check for no role mention */
        if (roles.isEmpty()) {
            event.getChannel()
                    .sendMessage(EmbedFactory.getCommandEmbedError(this, getString("no_role"))).get();
            return false;
        }
        role = roles.get(0);

         /* check for missing role manage permissions bot */
        if (!PermissionUtil.canManageRole(DiscordApiCollection.getInstance().getYourself(), role)) {
            event.getChannel()
                    .sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role", role.getMentionTag()))).get();
            return false;
        }

        /* check for missing role manage permissions user */
        if (!PermissionUtil.canManageRole(event.getMessageAuthor().asUser().get(), role)) {
            event.getChannel()
                    .sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role_user", role.getMentionTag()))).get();
            return false;
        }

        Optional<CompletableFuture<Boolean>> futureOpt = RoleAssigner.getInstance().assignRoles(event.getServer().get(), role, addRole());

        /* check for busy */
        if (!futureOpt.isPresent()) {
            event.getChannel()
                    .sendMessage(EmbedFactory.getCommandEmbedError(this, getString("busy_desc"), getString("busy_title"))).get();
            return false;
        }

        future = futureOpt.get();
        future.thenAccept(this::onAssignmentFinished);

        message = event.getChannel()
                .sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("loading", role.getMentionTag(), StringUtil.getLoadingReaction(event.getServerTextChannel().get()), CANCEL_EMOJI))).get();
        message.addReaction(CANCEL_EMOJI).get();

        return true;
    }

    protected boolean addRole() { return true; }

    private void onAssignmentFinished(boolean success) {
        removeReactionListener();
        try {
            if (success)
                message.edit(EmbedFactory.getCommandEmbedStandard(this, getString("success_desc", role.getMentionTag()))).get();
            else
                message.edit(EmbedFactory.getCommandEmbedError(this, getString("canceled_desc", role.getMentionTag()), getString("canceled_title"))).get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Exception in role assignment finished", e);
        }
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (event.getEmoji().equalsEmoji(CANCEL_EMOJI) && future != null) {
            removeReactionListener();
            RoleAssigner.getInstance().cancel(event.getServer().get().getId());
        }
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) {}
}
