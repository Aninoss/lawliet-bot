package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.RolesStateProcessor;
import core.EmbedFactory;
import core.ListGen;
import core.atomicassets.AtomicRole;
import mysql.hibernate.entity.BotLogEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
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

    public static final int STATE_SET_ROLES = 1;

    public StickyRolesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        checkRolesWithLog(event.getMember(), getGuildEntity().getStickyRoles().getRoles().stream().map(r -> r.get().orElse(null)).collect(Collectors.toList()));
        registerNavigationListener(event.getMember(), List.of(
                new RolesStateProcessor(this, STATE_SET_ROLES, DEFAULT_STATE, getString("state0_mroles"))
                        .setMinMax(0, MAX_ROLES)
                        .setCheckAccess(true)
                        .setLogEvent(BotLogEntity.Event.STICKY_ROLES)
                        .setGetter(() -> getGuildEntity().getStickyRoles().getRoleIds())
                        .setSetter(roleIds -> getGuildEntity().getStickyRoles().setRoleIds(roleIds))
        ));
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        return switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                yield false;
            }
            case 0 -> {
                setState(STATE_SET_ROLES);
                yield true;
            }
            default -> false;
        };
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) {
        setComponents(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                .addField(getString("state0_mroles"), new ListGen<AtomicRole>().getList(getGuildEntity().getStickyRoles().getRoles(), getLocale(), m -> m.getPrefixedNameInField(getLocale())), true);
    }

}
