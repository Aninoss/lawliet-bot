package modules;

import commands.runnables.managementcategory.AutoRolesCommand;
import core.CustomObservableList;
import core.CustomThread;
import core.DiscordApiCollection;
import core.PermissionCheckRuntime;
import mysql.modules.autoroles.AutoRolesBean;
import mysql.modules.autoroles.DBAutoRoles;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class AutoRolesRepair implements Runnable {

    private final static Logger LOGGER = LoggerFactory.getLogger(AutoRolesRepair.class);

    private boolean started = false;

    public void start() {
        if (started) return;
        started = true;

        new CustomThread(this, "autoroles_repair", 1).start();
    }

    @Override
    public void run() {
        for(Server server : DiscordApiCollection.getInstance().getServers()) {
            try {
                AutoRolesBean autoRolesBean = DBAutoRoles.getInstance().getBean(server.getId());
                Locale locale = autoRolesBean.getServerBean().getLocale();
                if (autoRolesBean.getRoleIds().size() > 0) {
                    CustomObservableList<Role> roles = autoRolesBean.getRoleIds().transform(server::getRoleById, DiscordEntity::getId);
                    server.getMembers().stream()
                            .filter(user -> userJoinedRecently(server, user))
                            .forEach(user -> checkRoles(locale, user, roles));
                }
            } catch (ExecutionException e) {
                LOGGER.error("Could not optain auto roles bean for server {}", server.getId());
            }
        }
    }

    private void checkRoles(Locale locale, User user, CustomObservableList<Role> roles) {
        roles.stream()
                .filter(role -> !role.hasUser(user) && PermissionCheckRuntime.getInstance().botCanManageRoles(locale, AutoRolesCommand.class, role))
                .forEach(role -> role.addUser(user).exceptionally(ExceptionLogger.get()));
    }

    private boolean userJoinedRecently(Server server, User user) {
        Optional<Instant> instantOpt = server.getJoinedAtTimestamp(user);
        if (instantOpt.isPresent()) {
            Instant instant = instantOpt.get();
            return instant.isAfter(Instant.now().minus(2, ChronoUnit.HOURS));
        }

        return false;
    }

}
