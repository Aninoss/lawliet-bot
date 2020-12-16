package modules.repair;

import commands.runnables.utilitycategory.AutoRolesCommand;
import constants.FisheryStatus;
import core.PermissionCheckRuntime;
import core.TaskQueue;
import mysql.modules.autoroles.AutoRolesBean;
import mysql.modules.autoroles.DBAutoRoles;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryServerBean;
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
import java.util.List;
import java.util.Locale;

public class RolesRepair {

    private final static Logger LOGGER = LoggerFactory.getLogger(RolesRepair.class);

    private static final RolesRepair ourInstance = new RolesRepair();
    public static RolesRepair getInstance() { return ourInstance; }
    private RolesRepair() { }

    private final TaskQueue taskQueue = new TaskQueue("roles_repair");

    public void start(DiscordApi api, int hours) {
        taskQueue.attach(() -> run(api, hours));
    }

    public void run(DiscordApi api, int hours) {
        for(Server server : api.getServers()) {
            processAutoRoles(server, hours);
            processFisheryRoles(server, hours);
        }
    }

    private void processFisheryRoles(Server server, int hours) {
        FisheryServerBean fisheryServerBean = DBFishery.getInstance().getBean(server.getId());
        Locale locale = fisheryServerBean.getServerBean().getLocale();
        if (fisheryServerBean.getServerBean().getFisheryStatus() != FisheryStatus.STOPPED && fisheryServerBean.getRoleIds().size() > 0) {
            server.getMembers().stream()
                    .filter(user -> !user.isBot() && userJoinedRecently(server, user, hours))
                    .forEach(user -> checkRoles(locale, user, fisheryServerBean.getUserBean(user.getId()).getRoles()));
        }
    }

    private void processAutoRoles(Server server, int hours) {
        AutoRolesBean autoRolesBean = DBAutoRoles.getInstance().getBean(server.getId());
        Locale locale = autoRolesBean.getServerBean().getLocale();
        if (autoRolesBean.getRoleIds().size() > 0) {
            List<Role> roles = autoRolesBean.getRoleIds().transform(server::getRoleById, DiscordEntity::getId);
            server.getMembers().stream()
                    .filter(user -> userJoinedRecently(server, user, hours))
                    .forEach(user -> checkRoles(locale, user, roles));
        }
    }

    private void checkRoles(Locale locale, User user, List<Role> roles) {
        roles.stream()
                .filter(role -> !user.getRoles(role.getServer()).contains(role) && PermissionCheckRuntime.getInstance().botCanManageRoles(locale, AutoRolesCommand.class, role))
                .forEach(role -> {
                    LOGGER.info("Giving role \"{}\" to user \"{}\" on server \"{}\"", role.getName(), user.getDiscriminatedName(), role.getServer().getName());
                    role.addUser(user).exceptionally(ExceptionLogger.get());
                });
    }

    private boolean userJoinedRecently(Server server, User user, int hours) {
        return server.getJoinedAtTimestamp(user)
                .map(instant -> instant.isAfter(Instant.now().minus(hours, ChronoUnit.HOURS)))
                .orElse(false);
    }

}
