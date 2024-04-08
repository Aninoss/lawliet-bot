package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.RolesStateProcessor;
import constants.LogStatus;
import core.CustomObservableList;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicRole;
import core.cache.ServerPatreonBoostCache;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import modules.RoleAssigner;
import mysql.hibernate.entity.BotLogEntity;
import mysql.modules.autoroles.AutoRolesData;
import mysql.modules.autoroles.DBAutoRoles;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "autoroles",
        botGuildPermissions = Permission.MANAGE_ROLES,
        userGuildPermissions = Permission.MANAGE_ROLES,
        emoji = "👪",
        executableWithoutArgs = true,
        aliases = {"basicroles", "autorole", "aroles", "joinroles", "jroles"}
)
public class AutoRolesCommand extends NavigationAbstract {

    public static final int MAX_ROLES = 12;

    public static final int STATE_SET_ROLES = 1;

    private CustomObservableList<AtomicRole> roles;

    public AutoRolesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        AutoRolesData autoRolesData = DBAutoRoles.getInstance().retrieve(event.getGuild().getIdLong());
        roles = AtomicRole.transformIdList(event.getGuild(), autoRolesData.getRoleIds());

        checkRolesWithLog(event.getMember(), roles.stream().map(r -> r.get().orElse(null)).collect(Collectors.toList()));
        registerNavigationListener(event.getMember(), List.of(
                new RolesStateProcessor(this, STATE_SET_ROLES, DEFAULT_STATE, getString("roles"))
                        .setMinMax(0, MAX_ROLES)
                        .setCheckAccess(true)
                        .setLogEvent(BotLogEntity.Event.AUTO_ROLES)
                        .setGetter(() -> this.roles.stream().map(AtomicRole::getIdLong).collect(Collectors.toList()))
                        .setSetter(roleIds -> {
                            roles.clear();
                            roles.addAll(roleIds.stream().map(roleId -> new AtomicRole(event.getGuild().getIdLong(), roleId)).collect(Collectors.toList()));
                        })
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
            case 1 -> {
                if (!ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
                    yield true;
                }

                List<Role> roleList = roles.stream().map(role -> role.get().orElse(null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                Optional<CompletableFuture<Boolean>> future = RoleAssigner.assignRoles(event.getGuild(), roleList, true, getLocale(), getClass());
                if (future.isEmpty()) {
                    setLog(LogStatus.FAILURE, getString("syncactive"));
                    yield true;
                }

                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.AUTO_ROLES_SYNC, event.getMember());
                getEntityManager().getTransaction().commit();

                FeatureLogger.inc(PremiumFeature.AUTO_ROLES_SYNC, event.getGuild().getIdLong());
                setLog(LogStatus.SUCCESS, getString("syncstart"));
                yield true;
            }
            default -> false;
        };
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawDefault(Member member) {
        setComponents(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                .addField(getString("state0_mroles"), new ListGen<AtomicRole>().getList(roles, getLocale(), m -> m.getPrefixedNameInField(getLocale())), true);
    }

}
