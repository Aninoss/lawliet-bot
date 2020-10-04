package modules.repair;

import commands.runnables.fisherysettingscategory.FisheryCommand;
import commands.runnables.managementcategory.AutoRolesCommand;
import constants.FisheryStatus;
import core.CustomObservableList;
import core.CustomThread;
import core.PermissionCheckRuntime;
import mysql.modules.autoroles.AutoRolesBean;
import mysql.modules.autoroles.DBAutoRoles;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryServerBean;
import mysql.modules.fisheryusers.FisheryUserBean;
import org.javacord.api.DiscordApi;
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

public class RolesRepair implements Runnable {

    private final static Logger LOGGER = LoggerFactory.getLogger(RolesRepair.class);

    private final DiscordApi api;

    public RolesRepair(DiscordApi api) {
        this.api = api;
    }

    public void start() {
        new CustomThread(this, "roles_repair", 1).start();
    }

    @Override
    public void run() {
        for(Server server : api.getServers()) {
            try {
                processAutoRoles(server);
                processFisheryRoles(server);
            } catch (ExecutionException e) {
                LOGGER.error("Could not optain auto roles bean for server {}", server.getId());
            }
        }
    }

    private void processFisheryRoles(Server server) throws ExecutionException {
        FisheryServerBean fisheryServerBean = DBFishery.getInstance().getBean(server.getId());
        Locale locale = fisheryServerBean.getServerBean().getLocale();
        if (fisheryServerBean.getServerBean().getFisheryStatus() != FisheryStatus.STOPPED && fisheryServerBean.getRoleIds().size() > 0) {
            server.getMembers().stream()
                    .filter(user -> !user.isBot() && userJoinedRecently(server, user))
                    .forEach(user -> checkRolesFisheryRoles(locale, user, fisheryServerBean.getUserBean(user.getId())));
        }
    }

    private void processAutoRoles(Server server) throws ExecutionException {
        AutoRolesBean autoRolesBean = DBAutoRoles.getInstance().getBean(server.getId());
        Locale locale = autoRolesBean.getServerBean().getLocale();
        if (autoRolesBean.getRoleIds().size() > 0) {
            CustomObservableList<Role> roles = autoRolesBean.getRoleIds().transform(server::getRoleById, DiscordEntity::getId);
            server.getMembers().stream()
                    .filter(user -> userJoinedRecently(server, user))
                    .forEach(user -> checkRolesAutoRoles(locale, user, roles));
        }
    }

    private void checkRolesFisheryRoles(Locale locale, User user, FisheryUserBean userBean) {
        userBean.getRoles().stream()
                .filter(role -> !role.hasUser(user) && PermissionCheckRuntime.getInstance().botCanManageRoles(locale, FisheryCommand.class, role))
                .forEach(role -> role.addUser(user).exceptionally(ExceptionLogger.get()));
    }

    private void checkRolesAutoRoles(Locale locale, User user, CustomObservableList<Role> roles) {
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
