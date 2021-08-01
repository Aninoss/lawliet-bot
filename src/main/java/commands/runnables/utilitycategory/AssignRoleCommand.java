package commands.runnables.utilitycategory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import constants.Emojis;
import core.EmbedFactory;
import core.TextManager;
import core.atomicassets.AtomicRole;
import core.utils.EmojiUtil;
import core.utils.MentionUtil;
import modules.RoleAssigner;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

@CommandProperties(
        trigger = "assignrole",
        userGuildPermissions = Permission.MANAGE_ROLES,
        botGuildPermissions = Permission.MANAGE_ROLES,
        emoji = "\uD83D\uDCE5",
        executableWithoutArgs = false,
        patreonRequired = true,
        turnOffTimeout = true,
        requiresMemberCache = true,
        aliases = { "giverole", "assign" }
)
public class AssignRoleCommand extends Command implements OnButtonListener {

    private static final String CANCEL_EMOJI = Emojis.X;

    private AtomicRole atomicRole;

    public AssignRoleCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        List<Role> roles = MentionUtil.getRoles(event.getMessage(), args).getList();

        /* check for no role mention */
        if (roles.isEmpty()) {
            event.getChannel().sendMessageEmbeds(
                    EmbedFactory.getEmbedError(this, getString("no_role")).build()
            ).queue();
            return false;
        }
        Role role = roles.get(0);
        atomicRole = new AtomicRole(role);

        /* check for missing role manage permissions bot */
        if (!event.getGuild().getSelfMember().canInteract(role)) {
            event.getChannel().sendMessageEmbeds(
                    EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role", false, role.getAsMention())).build()
            ).queue();
            return false;
        }

        /* check for missing role manage permissions user */
        if (!event.getMember().canInteract(role)) {
            event.getChannel().sendMessageEmbeds(
                    EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role_user", false, role.getAsMention())).build()
            ).queue();
            return false;
        }

        Optional<CompletableFuture<Boolean>> futureOpt = RoleAssigner.getInstance().assignRoles(role, addRole());

        /* check for busy */
        if (futureOpt.isEmpty()) {
            event.getChannel().sendMessageEmbeds(
                    EmbedFactory.getEmbedError(this, getString("busy_desc"), getString("busy_title")).build()
            ).queue();
            return false;
        }

        CompletableFuture<Boolean> future = futureOpt.get();
        future.thenAccept(this::onAssignmentFinished);

        setButtons(Button.of(ButtonStyle.SECONDARY, "quit", TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort")));
        registerButtonListener();
        return true;
    }

    protected boolean addRole() {
        return true;
    }

    private void onAssignmentFinished(boolean success) {
        deregisterListenersWithButtons();
        if (success) {
            drawMessage(EmbedFactory.getEmbedDefault(this, getString("success_desc", atomicRole.getAsMention())));
        } else {
            drawMessage(EmbedFactory.getEmbedError(this, getString("canceled_desc", atomicRole.getAsMention()), getString("canceled_title")));
        }
    }

    @Override
    public boolean onButton(ButtonClickEvent event) throws Throwable {
        event.deferEdit().queue();
        deregisterListenersWithButtons();
        RoleAssigner.getInstance().cancel(event.getGuild().getIdLong());
        return false;
    }

    @Override
    public EmbedBuilder draw() throws Throwable {
        return EmbedFactory.getEmbedDefault(
                this,
                getString("loading", atomicRole.getAsMention(), EmojiUtil.getLoadingEmojiMention(getTextChannel().orElse(null)), CANCEL_EMOJI)
        );
    }

}
