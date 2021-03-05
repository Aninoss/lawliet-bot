package commands.runnables.utilitycategory;

import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnReactionAddListener;
import constants.PermissionDeprecated;
import core.EmbedFactory;
import core.ShardManager;
import core.MainLogger;
import core.TextManager;
import core.utils.JDAUtil;
import core.utils.MentionUtil;
import core.utils.BotPermissionUtil;
import modules.RoleAssigner;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "assignrole",
        userPermissions = PermissionDeprecated.MANAGE_ROLES,
        botPermissions = PermissionDeprecated.MANAGE_ROLES,
        emoji = "\uD83D\uDCE5",
        executableWithoutArgs = false,
        patreonRequired = true,
        turnOffTimeout = true,
        aliases = { "giverole", "assign" }
)
public class AssignRoleCommand extends Command implements OnReactionAddListener {

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
                    .sendMessage(EmbedFactory.getEmbedError(this, getString("no_role"))).get();
            return false;
        }
        role = roles.get(0);

         /* check for missing role manage permissions bot */
        if (!BotPermissionUtil.canManageRole(ShardManager.getInstance().getSelf(), role)) {
            event.getChannel()
                    .sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role", role.getMentionTag()))).get();
            return false;
        }

        /* check for missing role manage permissions user */
        if (!BotPermissionUtil.canManageRole(event.getMessageAuthor().asUser().get(), role)) {
            event.getChannel()
                    .sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role_user", role.getMentionTag()))).get();
            return false;
        }

        Optional<CompletableFuture<Boolean>> futureOpt = RoleAssigner.getInstance().assignRoles(event.getServer().get(), role, addRole());

        /* check for busy */
        if (futureOpt.isEmpty()) {
            event.getChannel()
                    .sendMessage(EmbedFactory.getEmbedError(this, getString("busy_desc"), getString("busy_title"))).get();
            return false;
        }

        future = futureOpt.get();
        future.thenAccept(this::onAssignmentFinished);

        message = event.getChannel()
                .sendMessage(EmbedFactory.getEmbedDefault(this, getString("loading", role.getMentionTag(), JDAUtil.getLoadingReaction(event.getServerTextChannel().get()), CANCEL_EMOJI))).get();
        message.addReaction(CANCEL_EMOJI).get();

        return true;
    }

    protected boolean addRole() { return true; }

    private void onAssignmentFinished(boolean success) {
        removeReactionListener();
        try {
            if (success)
                message.edit(EmbedFactory.getEmbedDefault(this, getString("success_desc", role.getMentionTag()))).get();
            else
                message.edit(EmbedFactory.getEmbedError(this, getString("canceled_desc", role.getMentionTag()), getString("canceled_title"))).get();
        } catch (InterruptedException | ExecutionException e) {
            MainLogger.get().error("Exception in role assignment finished", e);
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
