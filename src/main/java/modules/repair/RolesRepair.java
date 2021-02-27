package modules.repair;

import commands.runnables.utilitycategory.AutoRolesCommand;
import constants.FisheryStatus;
import core.PermissionCheckRuntime;
import mysql.modules.autoroles.AutoRolesBean;
import mysql.modules.autoroles.DBAutoRoles;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryServerBean;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RolesRepair {

    private final static Logger LOGGER = LoggerFactory.getLogger(RolesRepair.class);

    private static final RolesRepair ourInstance = new RolesRepair();

    public static RolesRepair getInstance() {
        return ourInstance;
    }

    private RolesRepair() {
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public void start(DiscordApi api, int minutes) {
        executorService.submit(() -> run(api, minutes));
    }

    public void run(DiscordApi api, int minutes) {
        for (Server server : api.getServers()) {
            processAutoRoles(server, minutes);
            processFisheryRoles(server, minutes);
        }
    }

    private void processFisheryRoles(Server server, int minutes) {
        FisheryServerBean fisheryServerBean = DBFishery.getInstance().getBean(server.getId());
        Locale locale = fisheryServerBean.getServerBean().getLocale();
        if (fisheryServerBean.getServerBean().getFisheryStatus() != FisheryStatus.STOPPED && fisheryServerBean.getRoleIds().size() > 0) {
            server.getMembers().stream()
                    .filter(user -> !user.isBot() && userJoinedRecently(server, user, minutes))
                    .forEach(user -> checkRoles(locale, user, fisheryServerBean.getUserBean(user.getId()).getRoles()));
        }
    }

    private void processAutoRoles(Server server, int minutes) {
        AutoRolesBean autoRolesBean = DBAutoRoles.getInstance().getBean(server.getId());
        Locale locale = autoRolesBean.getServerBean().getLocale();
        if (autoRolesBean.getRoleIds().size() > 0) {
            List<Role> roles = autoRolesBean.getRoleIds().transform(server::getRoleById, DiscordEntity::getId);
            server.getMembers().stream()
                    .filter(user -> userJoinedRecently(server, user, minutes))
                    .forEach(user -> checkRoles(locale, user, roles));
        }
    }

    private void checkRoles(Locale locale, User user, List<Role> roles) {
        roles.stream()
                .filter(role -> !user.getRoles(role.getServer()).contains(role) && PermissionCheckRuntime.getInstance().botCanManageRoles(locale, AutoRolesCommand.class, role))
                .forEach(role -> {
                    LOGGER.info("Giving role \"{}\" to user \"{}\" on server \"{}\"", role.getName(), user.getDiscriminatedName(), role.getServer().getName());
                    try {
                        role.addUser(user).get();
                    } catch (InterruptedException | ExecutionException e) {
                        LOGGER.error("Exception", e);
                    }
                });
    }

    private boolean userJoinedRecently(Server server, User user, int minutes) {
        return server.getJoinedAtTimestamp(user)
                .map(instant -> instant.isAfter(Instant.now().minus(minutes, ChronoUnit.MINUTES)))
                .orElse(false);
    }

}
