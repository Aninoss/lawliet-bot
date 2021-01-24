package core.utils;

import constants.Permission;
import core.EmbedFactory;
import core.DiscordApiManager;
import core.TextManager;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionState;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class PermissionUtil {

    public static EmbedBuilder getUserAndBotPermissionMissingEmbed(Locale locale, Server server, ServerChannel channel, User user, int userPermissions, int botPermissions) {
        ArrayList<Integer> userPermission = getMissingPermissionListForUser(server, channel, user, userPermissions);
        ArrayList<Integer> botPermission = getMissingPermissionListForUser(server, channel, DiscordApiManager.getInstance().getYourself(), botPermissions);

        return getUserPermissionMissingEmbed(locale, userPermission, botPermission);
    }

    public static ArrayList<Integer> getMissingPermissionListForUser(Server server, User user, int userPermissions) {
        return getMissingPermissionListForUser(server, null, user, userPermissions);
    }

    public static ArrayList<Integer> getMissingPermissionListForUser(Server server, ServerChannel channel, User user, int userPermissions) {
        userPermissions |= Permission.READ_MESSAGES;
        ArrayList<Integer> missingPermissions = new ArrayList<>();
        if (hasAdminPermissions(server, user))
            return missingPermissions;

        for(int permission: permissionsToNumberList(userPermissions)) {
            PermissionConvertion permissionConvertion = convertPermission(permission);
            Permissions permissions;

            if (channel != null &&
                    (channel instanceof ChannelCategory || !permissionConvertion.isServerOnly())
            ) {
                permissions = channel.getEffectivePermissions(user);
            } else {
                permissions = server.getPermissions(user);
            }

            if (permissions.getState(permissionConvertion.getPermissionType()) != PermissionState.ALLOWED)
                missingPermissions.add(permission);
        }

        return missingPermissions;
    }

    private static PermissionConvertion convertPermission(int permission) {
        switch (0x1 << (permission - 1)) {
            case Permission.ADMINISTRATOR: return new PermissionConvertion(PermissionType.ADMINISTRATOR, true);
            case Permission.ATTACH_FILES: return new PermissionConvertion(PermissionType.ATTACH_FILE, false);
            case Permission.KICK_MEMBERS: return new PermissionConvertion(PermissionType.KICK_MEMBERS, true);
            case Permission.BAN_MEMBERS: return new PermissionConvertion(PermissionType.BAN_MEMBERS, true);
            case Permission.CHANGE_OWN_NICKNAME: return new PermissionConvertion(PermissionType.CHANGE_NICKNAME, true);
            case Permission.READ_MESSAGES: return new PermissionConvertion(PermissionType.READ_MESSAGES, false);
            case Permission.MANAGE_CHANNELS_ON_SERVER: return new PermissionConvertion(PermissionType.MANAGE_CHANNELS, true);
            case Permission.MANAGE_CHANNEL: return new PermissionConvertion(PermissionType.MANAGE_CHANNELS, false);
            case Permission.DEAFEN_MEMBERS: return new PermissionConvertion(PermissionType.DEAFEN_MEMBERS, true);
            case Permission.MANAGE_EMOJIS: return new PermissionConvertion(PermissionType.MANAGE_EMOJIS, true);
            case Permission.EMBED_LINKS: return new PermissionConvertion(PermissionType.EMBED_LINKS, false);
            case Permission.MANAGE_MESSAGES: return new PermissionConvertion(PermissionType.MANAGE_MESSAGES, false);
            case Permission.MANAGE_NICKNAMES: return new PermissionConvertion(PermissionType.MANAGE_NICKNAMES, true);
            case Permission.MANAGE_ROLES: return new PermissionConvertion(PermissionType.MANAGE_ROLES, true);
            case Permission.MANAGE_CHANNEL_PERMISSIONS: return new PermissionConvertion(PermissionType.MANAGE_ROLES, false);
            case Permission.MANAGE_SERVER: return new PermissionConvertion(PermissionType.MANAGE_SERVER, true);
            case Permission.MENTION_EVERYONE: return new PermissionConvertion(PermissionType.MENTION_EVERYONE, false);
            case Permission.MOVE_MEMBERS: return new PermissionConvertion(PermissionType.MOVE_MEMBERS, false);
            case Permission.MUTE_MEMBERS: return new PermissionConvertion(PermissionType.MUTE_MEMBERS, true);
            case Permission.READ_MESSAGE_HISTORY: return new PermissionConvertion(PermissionType.READ_MESSAGE_HISTORY, false);
            case Permission.SEND_MESSAGES: return new PermissionConvertion(PermissionType.SEND_MESSAGES, false);
            case Permission.VIEW_AUDIT_LOG: return new PermissionConvertion(PermissionType.VIEW_AUDIT_LOG, true);
            case Permission.SEND_TTS_MESSAGES: return new PermissionConvertion(PermissionType.SEND_TTS_MESSAGES, false);
            case Permission.USE_EXTERNAL_EMOJIS: return new PermissionConvertion(PermissionType.USE_EXTERNAL_EMOJIS, false);
            case Permission.ADD_REACTIONS: return new PermissionConvertion(PermissionType.ADD_REACTIONS, false);
            case Permission.MANAGE_WEBHOOKS: return new PermissionConvertion(PermissionType.MANAGE_WEBHOOKS, true);
            case Permission.CREATE_INSTANT_INVITE: return new PermissionConvertion(PermissionType.CREATE_INSTANT_INVITE, false);
            case Permission.CONNECT: return new PermissionConvertion(PermissionType.CONNECT, false);
            case Permission.CONNECT_ON_SERVER: return new PermissionConvertion(PermissionType.CONNECT, true);
        }

        throw new RuntimeException("Faulty permission!");
    }

    public static EmbedBuilder getUserPermissionMissingEmbed(Locale locale, ArrayList<Integer> userPermission, ArrayList<Integer> botPermission) {
        EmbedBuilder eb = null;
        boolean alright = userPermission.size() == 0 && botPermission.size() == 0;
        if (!alright) {
            eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_title"));

            if (userPermission.size()>0) {
                StringBuilder desc = new StringBuilder();
                for(int i: userPermission) {
                    desc.append("• ");
                    desc.append(TextManager.getString(locale, TextManager.PERMISSIONS, String.valueOf(i)));
                    desc.append("\n");
                }
                eb.addField(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_you"), desc.toString());
            }

            if (botPermission.size()>0) {
                StringBuilder desc = new StringBuilder();
                for(int i: botPermission) {
                    desc.append("• ");
                    desc.append(TextManager.getString(locale, TextManager.PERMISSIONS, String.valueOf(i)));
                    desc.append("\n");
                }
                eb.addField(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_bot"), desc.toString());
            }
        }
        return eb;
    }

    public static ArrayList<Integer> permissionsToNumberList(int permissions) {
        ArrayList<Integer> list = new ArrayList<>();

        int i = 1;
        for(int check = 0x1; check <= Permission.MAX; check <<= 1) {
            if ((permissions & check) > 0) {
                list.add(i);
            }
            i++;
        }

        return list;
    }

    public static boolean canYouManageRole(Role role) {
        return canManageRole(DiscordApiManager.getInstance().getYourself(), role);
    }

    public static boolean canManageRole(User user, Role role) {
        Server server = role.getServer();
        if (role.isManaged() || !server.canManageRoles(user)) return false;
        if (server.getOwnerId() == user.getId()) return true;

        int highestPosition = -1;
        for(Role ownRole: server.getRoles(user)) {
            if (ownRole.getPermissions().getState(PermissionType.MANAGE_ROLES) == PermissionState.ALLOWED || ownRole.getPermissions().getState(PermissionType.ADMINISTRATOR) == PermissionState.ALLOWED) {
                highestPosition = Math.max(highestPosition, ownRole.getPosition());
            }
        }

        return highestPosition > role.getPosition();
    }

    public static boolean hasAdminPermissions(Server server, User user) {
        return server.getAllowedPermissions(user).contains(PermissionType.ADMINISTRATOR) || user.getId() == server.getOwnerId();
    }

    public static boolean canYouKickUser(Server server, User user) {
        return server.canYouKickUser(user) && server.getOwnerId() != user.getId();
    }

    public static boolean canYouBanUser(Server server, User user) {
        return server.canYouBanUser(user) && server.getOwnerId() != user.getId();
    }

    public static boolean botHasServerPermission(Server server, PermissionType permissionType) {
        return userHasServerPermission(server, DiscordApiManager.getInstance().getYourself(), permissionType);
    }

    public static boolean botHasChannelPermission(ServerChannel channel, PermissionType permissionType) {
        return userHasChannelPermission(channel, DiscordApiManager.getInstance().getYourself(), permissionType);
    }

    public static boolean userHasServerPermission(Server server, User user, PermissionType permissionType) {
        if (hasAdminPermissions(server, user))
            return true;

        return server.getAllowedPermissions(user).contains(permissionType);
    }

    public static boolean userHasChannelPermission(ServerChannel channel, User user, PermissionType permissionType) {
        if (hasAdminPermissions(channel.getServer(), user)) return true;
        if (channel.getEffectivePermissions(user).getState(permissionType) == PermissionState.ALLOWED) return true;
        return channel.getEffectivePermissions(user).getState(permissionType) == PermissionState.UNSET && channel.getServer().getAllowedPermissions(user).contains(permissionType);
    }

    public static boolean roleHasServerPermission(Role role, PermissionType permissionType) {
        Permissions permissions = role.getPermissions();
        return permissions.getState(PermissionType.ADMINISTRATOR) == PermissionState.ALLOWED ||
                permissions.getState(permissionType) == PermissionState.ALLOWED;
    }

    public static boolean roleHasChannelPermission(ServerChannel channel, Role role, PermissionType permissionType) {
        Permissions permissions = channel.getOverwrittenPermissions(role);
        return roleHasServerPermission(role, PermissionType.ADMINISTRATOR) ||
                (permissions.getState(permissionType) == PermissionState.UNSET && roleHasServerPermission(role, permissionType)) ||
                permissions.getState(permissionType) == PermissionState.ALLOWED;
    }

    public static boolean roleHasChannelPermissionRestricted(ServerChannel channel, Role role, PermissionType permissionType) {
        return !roleHasServerPermission(role, PermissionType.ADMINISTRATOR) &&
                channel.getOverwrittenPermissions(role).getState(permissionType) == PermissionState.DENIED;
    }

    public static List<Role> getMemberRoles(Server server) {
        int min = (int) (server.getMemberCount() * 0.75);
        return server.getRoles().stream()
                .filter(role -> role.getUsers().size() >= min)
                .collect(Collectors.toList());
    }

    public static boolean channelIsPublic(ServerTextChannel channel) {
        Server server = channel.getServer();
        return server.getMembers().stream()
                .filter(user -> channel.canSee(user) && channel.canReadMessageHistory(user))
                .count() >= server.getMemberCount() * 0.75;
    }

    public static boolean userCanMentionRoles(ServerTextChannel channel, User user, String messageContent) {
        if (channel.canMentionEveryone(user))
            return true;

        if (messageContent.contains("@everyone") || messageContent.contains("@here"))
            return false;

        return channel.getServer().getRoles().stream()
                .filter(role -> messageContent.contains(role.getMentionTag()))
                .allMatch(Role::isMentionable);
    }

    public static class PermissionConvertion {

        private final PermissionType permissionType;
        private final boolean serverOnly;

        public PermissionConvertion(PermissionType permissionType, boolean serverOnly) {
            this.permissionType = permissionType;
            this.serverOnly = serverOnly;
        }

        public PermissionType getPermissionType() {
            return permissionType;
        }

        public boolean isServerOnly() {
            return serverOnly;
        }

    }

}
