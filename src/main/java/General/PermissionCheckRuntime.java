package General;

import Constants.Permission;
import org.javacord.api.entity.Nameable;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class PermissionCheckRuntime {

    private static PermissionCheckRuntime instance = new PermissionCheckRuntime();
    private static final int PERMISSION_ROLE_POS = -1;

    private Map<Long, Map<Integer, Instant>> errorTimes = new HashMap<>();

    private PermissionCheckRuntime() {}
    public static PermissionCheckRuntime getInstance() {
        return instance;
    }

    public boolean botHasPermission(Locale locale, String commandTrigger, Server server, int permissions) {
        return botHasPermission(locale, commandTrigger, server, null, permissions);
    }

    public boolean botHasPermission(Locale locale, String commandTrigger, ServerChannel channel, int permissions) {
        return botHasPermission(locale, commandTrigger, channel.getServer(), channel, permissions);
    }

    private boolean botHasPermission(Locale locale, String commandTrigger, Server server, ServerChannel channel, int permissions) {
        ArrayList<Integer> missingPermissions = PermissionCheck.getMissingPermissionListForUser(server, channel, server.getApi().getYourself(), permissions);

        if (missingPermissions.size() == 0) return true;

        if (canPostError(server, permissions) && canContactOwner(server)) {
            try {
                String permissionsList = new ListGen<Integer>().getList(missingPermissions, ListGen.SLOT_TYPE_BULLET, n -> {
                    try {
                        return "**"+TextManager.getString(locale, TextManager.PERMISSIONS, String.valueOf(n))+"**";
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return "";
                });
                EmbedBuilder eb = EmbedFactory.getEmbedError();
                eb.setTitle(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_title"));
                eb.setDescription(TextManager.getString(locale, TextManager.GENERAL, "permission_runtime", channel != null, commandTrigger, channel != null ? (channel.asServerTextChannel().isPresent() ? "#" : "") + channel.getName() : "", permissionsList));

                if (Tools.canSendPrivateMessage(server.getOwner()))server.getOwner().sendMessage(eb).get();
            } catch (IOException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            setErrorInstant(server, permissions);
        }

        return false;
    }

    public boolean botCanManageRoles(Locale locale, String commandTrigger, List<Role> roles) {
        return botCanManageRoles(locale, commandTrigger, roles.toArray(new Role[0]));
    }

    public boolean botCanManageRoles(Locale locale, String commandTrigger, Role... roles) {
        ArrayList<Role> unreachableRoles = new ArrayList<>();

        for(Role role: roles) {
            if (!Tools.canManageRole(role)) unreachableRoles.add(role);
        }

        if (unreachableRoles.size() == 0) return true;

        Server server = roles[0].getServer();
        if (botHasPermission(locale, commandTrigger, server, Permission.MANAGE_ROLES_ON_SERVER) && canPostError(server, PERMISSION_ROLE_POS) && canContactOwner(server)) {
            try {
                String rolesList = new ListGen<Role>().getList(unreachableRoles, ListGen.SLOT_TYPE_BULLET, role -> "**@"+role.getName()+"**");
                EmbedBuilder eb = EmbedFactory.getEmbedError();
                eb.setTitle(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_title"));
                eb.setDescription(TextManager.getString(locale, TextManager.GENERAL, "permission_runtime_rolespos", commandTrigger, rolesList));

                if (Tools.canSendPrivateMessage(server.getOwner())) server.getOwner().sendMessage(eb).get();
            } catch (IOException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            setErrorInstant(server, PERMISSION_ROLE_POS);
        }

        return false;
    }

    private boolean canContactOwner(Server server) {
        return canPostError(server, Permission.MANAGE_ROLES_ON_SERVER) && server.getOwner() != null && Tools.canSendPrivateMessage(server.getOwner());
    }

    private boolean canPostError(Server server, int permission) {
        Instant instant = errorTimes.computeIfAbsent(server.getId(), k -> new HashMap<>()).get(permission);
        return instant == null || instant.plusSeconds(15 * 60).isBefore(Instant.now());
    }

    private void setErrorInstant(Server server, int permission) {
        errorTimes.computeIfAbsent(server.getId(), k -> new HashMap<>()).put(permission, Instant.now());
    }

}
