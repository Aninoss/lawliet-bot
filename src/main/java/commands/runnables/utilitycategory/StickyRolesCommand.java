package commands.runnables.utilitycategory;

import commands.CommandEvent;
import commands.NavigationHelper;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import core.EmbedFactory;
import core.ListGen;
import core.atomicassets.AtomicRole;
import core.utils.MentionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "stickyroles",
        botGuildPermissions = Permission.MANAGE_ROLES,
        userGuildPermissions = Permission.MANAGE_ROLES,
        emoji = "📍",
        executableWithoutArgs = true,
        aliases = { "keeproles", "stickyrole", "keeprole" }
)
public class StickyRolesCommand extends NavigationAbstract {

    public static final int MAX_ROLES = 12;

    private NavigationHelper<AtomicRole> roleNavigationHelper;

    public StickyRolesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        roleNavigationHelper = new NavigationHelper<>(this, guildEntity -> guildEntity.getStickyRoles().getRoles(), AtomicRole.class, MAX_ROLES);
        checkRolesWithLog(event.getMember(), getGuildEntity().getStickyRoles().getRoles().stream().map(r -> r.get().orElse(null)).collect(Collectors.toList()));
        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input, int state) {
        if (state == 1) {
            List<Role> roleList = MentionUtil.getRoles(event.getGuild(), input).getList();
            return roleNavigationHelper.addData(AtomicRole.from(roleList), input, event.getMember(), 0);
        }

        return null;
    }

    @Override
    public boolean controllerButton(ButtonInteractionEvent event, int i, int state) {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        deregisterListenersWithComponentMessage();
                        return false;

                    case 0:
                        roleNavigationHelper.startDataAdd(1);
                        return true;

                    case 1:
                        roleNavigationHelper.startDataRemove(2);
                        return true;

                    default:
                        return false;
                }

            case 1:
                if (i == -1) {
                    setState(0);
                    return true;
                }
                return false;

            case 2:
                return roleNavigationHelper.removeData(i, 0);

            default:
                return false;
        }
    }

    @Override
    public EmbedBuilder draw(Member member, int state) {
        return switch (state) {
            case 0 -> {
                setComponents(getString("state0_options").split("\n"));
                yield EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_mroles"), new ListGen<AtomicRole>().getList(getGuildEntity().getStickyRoles().getRoles(), getLocale(), m -> m.getPrefixedNameInField(getLocale())), true);
            }
            case 1 -> roleNavigationHelper.drawDataAdd();
            case 2 -> roleNavigationHelper.drawDataRemove(getLocale());
            default -> null;
        };
    }

}
