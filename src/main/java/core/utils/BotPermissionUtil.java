package core.utils;

import core.EmbedFactory;
import core.MemberCacheController;
import core.TextManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.managers.channel.attribute.IPermissionContainerManager;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.util.*;
import java.util.stream.Collectors;

public class BotPermissionUtil {

    public static EmbedBuilder getUserAndBotPermissionMissingEmbed(Locale locale, GuildChannel channel, Member member,
                                                                   Permission[] userGuildPermissions, Permission[] userChannelPermissions,
                                                                   Permission[] botGuildPermissions, Permission[] botChannelPermissions
    ) {
        List<Permission> userPermission = new ArrayList<>(getMissingPermissions(member, userGuildPermissions));
        userPermission.addAll(getMissingPermissions(channel, member, userChannelPermissions));

        List<Permission> botPermission = new ArrayList<>(getMissingPermissions(channel.getGuild().getSelfMember(), botGuildPermissions));
        botPermission.addAll(getMissingPermissions(channel, channel.getGuild().getSelfMember(), botChannelPermissions));

        return getUserPermissionMissingEmbed(locale, userPermission, botPermission);
    }

    public static EmbedBuilder getUserAndBotPermissionMissingEmbed(Locale locale, Member member,
                                                                   Permission[] userGuildPermissions, Permission[] botGuildPermissions
    ) {
        return getUserPermissionMissingEmbed(
                locale,
                getMissingPermissions(member, userGuildPermissions),
                getMissingPermissions(member.getGuild().getSelfMember(), botGuildPermissions)
        );
    }

    public static String getBotPermissionsMissingText(Locale locale, GuildChannel channel, Permission... permissions) {
        List<Permission> missing = getMissingPermissions(channel, channel.getGuild().getSelfMember(), permissions);
        if (missing.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_bot"))
                .append(" ");

        for (int i = 0; i < missing.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(TextManager.getString(locale, TextManager.PERMISSIONS, missing.get(i).name()));
        }

        return sb.toString();
    }

    public static EmbedBuilder getBotPermissionMissingEmbed(Locale locale, GuildChannel channel, Permission[] botGuildPermissions, Permission[] botChannelPermissions) {
        List<Permission> botPermission = new ArrayList<>(getMissingPermissions(channel.getGuild().getSelfMember(), botGuildPermissions));
        botPermission.addAll(getMissingPermissions(channel, channel.getGuild().getSelfMember(), botChannelPermissions));
        return getUserPermissionMissingEmbed(locale, Collections.emptyList(), botPermission);
    }

    public static List<Permission> getMissingPermissions(Member member, Permission... permissions) {
        return Arrays.stream(permissions)
                .filter(permission -> !member.hasPermission(permission))
                .collect(Collectors.toList());
    }

    public static List<Permission> getMissingPermissions(GuildChannel channel, Member member, Permission... permissions) {
        permissions = Arrays.copyOf(permissions, permissions.length + 1);
        permissions[permissions.length - 1] = Permission.VIEW_CHANNEL;

        return Arrays.stream(permissions)
                .filter(permission -> !member.hasPermission(channel, permission))
                .collect(Collectors.toList());
    }

    public static List<Permission> getMissingPermissions(GuildChannel channel, Role role, Permission... permissions) {
        return Arrays.stream(permissions)
                .filter(permission -> !role.getPermissionsExplicit(channel).contains(permission))
                .collect(Collectors.toList());
    }

    public static EmbedBuilder getUserPermissionMissingEmbed(Locale locale, List<Permission> userPermissions, List<Permission> botPermissions) {
        EmbedBuilder eb = null;
        if (!userPermissions.isEmpty() || !botPermissions.isEmpty()) {
            eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_title"));

            if (!userPermissions.isEmpty()) {
                StringBuilder desc = new StringBuilder();
                for (Permission permission : userPermissions) {
                    desc.append("- ");
                    desc.append(TextManager.getString(locale, TextManager.PERMISSIONS, permission.name()));
                    desc.append("\n");
                }
                eb.addField(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_you"), desc.toString(), false);
            }

            if (!botPermissions.isEmpty()) {
                StringBuilder desc = new StringBuilder();
                for (Permission permission : botPermissions) {
                    desc.append("- ");
                    desc.append(TextManager.getString(locale, TextManager.PERMISSIONS, permission.name()));
                    desc.append("\n");
                }
                eb.addField(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_bot"), desc.toString(), false);
            }
        }

        return eb;
    }

    public static boolean canKick(Member memberToKick) {
        return memberToKick.getGuild().getSelfMember().hasPermission(Permission.KICK_MEMBERS) &&
                memberToKick.getGuild().getSelfMember().canInteract(memberToKick);
    }

    public static boolean canKick(Member member, Member memberToKick) {
        return member.hasPermission(Permission.KICK_MEMBERS) &&
                member.canInteract(memberToKick);
    }

    public static boolean canBan(Member memberToBan) {
        return memberToBan.getGuild().getSelfMember().hasPermission(Permission.BAN_MEMBERS) &&
                memberToBan.getGuild().getSelfMember().canInteract(memberToBan);
    }

    public static boolean canBan(Member member, Member memberToBan) {
        return member.hasPermission(Permission.BAN_MEMBERS) &&
                member.canInteract(memberToBan);
    }

    public static boolean can(Guild guild, Permission... permissions) {
        return Arrays.stream(permissions)
                .allMatch(permission -> guild.getSelfMember().hasPermission(permissions));
    }

    public static boolean can(GuildChannel channel, Permission... permissions) {
        return channel.getGuild().getSelfMember().hasPermission(channel, Permission.VIEW_CHANNEL) &&
                Arrays.stream(permissions)
                        .allMatch(permission -> channel.getGuild().getSelfMember().hasPermission(channel, permissions));
    }

    public static boolean can(Member member, Permission... permissions) {
        return Arrays.stream(permissions)
                .allMatch(permission -> member.hasPermission(permissions));
    }

    public static boolean can(Member member, GuildChannel channel, Permission... permissions) {
        return member.hasPermission(channel, Permission.VIEW_CHANNEL) &&
                Arrays.stream(permissions)
                        .allMatch(permission -> member.hasPermission(channel, permissions));
    }

    public static boolean canReadHistory(GuildChannel channel, Permission... permissions) {
        return channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_HISTORY) &&
                can(channel, permissions);
    }

    public static boolean canWrite(GuildChannel channel, Permission... permissions) {
        return channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_SEND) &&
                can(channel, permissions);
    }

    public static boolean canWrite(Member member, GuildChannel channel, Permission... permissions) {
        return member.hasPermission(channel, Permission.MESSAGE_SEND) &&
                can(member, channel, permissions);
    }

    public static boolean canWriteEmbed(GuildChannel channel, Permission... permissions) {
        return channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS) &&
                canWrite(channel, permissions);
    }

    public static boolean canWriteEmbed(Member member, GuildChannel channel, Permission... permissions) {
        return member.hasPermission(channel, Permission.MESSAGE_EMBED_LINKS) &&
                canWrite(member, channel, permissions);
    }

    public static boolean canInteract(Guild guild, User targetUser) {
        return canInteract(guild, targetUser.getIdLong());
    }

    public static boolean canInteract(Guild guild, long targetUserId) {
        return canInteract(guild.getSelfMember(), targetUserId);
    }

    public static boolean canInteract(Member member, User targetUser) {
        return canInteract(member, targetUser.getIdLong());
    }

    public static boolean canInteract(Member member, long targetUserId) {
        Member target = MemberCacheController.getInstance().loadMember(member.getGuild(), targetUserId).join();
        if (target != null) {
            return member.canInteract(target);
        } else {
            return true;
        }
    }

    public static boolean canManage(GuildChannel channel, Permission permission) {
        if (channel.getGuild().getSelfMember().hasPermission(Permission.ADMINISTRATOR)) {
            return true;
        }

        Category category = null;
        if (channel instanceof ICategorizableChannel) {
            category = ((ICategorizableChannel) channel).getParentCategory();
        }

        return permission.getOffset() != Permission.MANAGE_PERMISSIONS.getOffset() &&
                (category == null || channel.getGuild().getSelfMember().hasPermission(category, permission)) &&
                channel.getGuild().getSelfMember().hasPermission(permission);
    }

    public static boolean canManage(Role role) {
        return canManage(role.getGuild().getSelfMember(), role);
    }

    public static boolean canManage(Member member, Role role) {
        return member.canInteract(role) &&
                !role.isManaged() &&
                !role.isPublicRole();
    }

    public static List<Role> getMemberRoles(Guild guild) {
        MemberCacheController.getInstance().loadMembersFull(guild).join();
        int min = (int) (guild.getMemberCount() * 0.75);
        return guild.getRoles().stream()
                .filter(role -> role.getGuild().getMembersWithRoles(role).size() >= min)
                .collect(Collectors.toList());
    }

    public static boolean channelIsPublic(GuildChannel channel) {
        Guild guild = channel.getGuild();
        MemberCacheController.getInstance().loadMembersFull(guild).join();
        return guild.getMembers().stream()
                .filter(member -> member.hasAccess(channel))
                .count() >= guild.getMemberCount() * 0.75;
    }

    public static boolean memberCanMentionRoles(GuildMessageChannel channel, Member member, String messageContent) {
        if (member.hasPermission(channel, Permission.MESSAGE_MENTION_EVERYONE)) {
            return true;
        }

        if (messageContent.contains("@everyone") || messageContent.contains("@here")) {
            return false;
        }

        return channel.getGuild().getRoles().stream()
                .filter(role -> messageContent.contains(role.getAsMention()))
                .allMatch(Role::isMentionable);
    }

    public static <T extends GuildChannel> ChannelAction<T> copyPermissions(ICategorizableChannel parentChannel, ChannelAction<T> channelAction) {
        for (PermissionOverride permissionOverride : parentChannel.getPermissionOverrides()) {
            channelAction = addPermission(parentChannel, channelAction, permissionOverride, true);
        }
        return channelAction;
    }

    public static <T extends GuildChannel> ChannelAction<T> addPermission(ICategorizableChannel parentChannel, ChannelAction<T> channelAction,
                                                                          IPermissionHolder permissionHolder, boolean allow, Permission... permissions
    ) {
        return addPermission(parentChannel, channelAction, parentChannel.getPermissionOverride(permissionHolder), allow,
                permissionHolder instanceof Member, permissionHolder.getIdLong(), permissions);
    }

    public static <T extends GuildChannel> ChannelAction<T> addPermission(ICategorizableChannel parentChannel, ChannelAction<T> channelAction,
                                                                          PermissionOverride permissionOverride, boolean allow, Permission... permissions
    ) {
        return addPermission(parentChannel, channelAction, permissionOverride, allow,
                permissionOverride.isMemberOverride(), permissionOverride.getIdLong(), permissions);
    }

    private static <T extends GuildChannel> ChannelAction<T> addPermission(ICategorizableChannel parentChannel, ChannelAction<T> channelAction,
                                                                          PermissionOverride permissionOverride, boolean allow, boolean memberOverride,
                                                                          long id, Permission... permissions
    ) {
        long allowRaw = 0L;
        long denyRaw = 0L;

        if (permissionOverride != null) {
            allowRaw |= permissionOverride.getAllowedRaw();
            denyRaw |= permissionOverride.getDeniedRaw();
        }

        if (allow) {
            allowRaw |= Permission.getRaw(permissions);
            denyRaw &= ~Permission.getRaw(permissions);
        } else {
            allowRaw &= ~Permission.getRaw(permissions);
            denyRaw |= Permission.getRaw(permissions);
        }

        List<Permission> allowList = Permission.getPermissions(allowRaw).stream()
                .filter(permission -> BotPermissionUtil.canManage(parentChannel, permission))
                .collect(Collectors.toList());

        List<Permission> denyList = Permission.getPermissions(denyRaw).stream()
                .filter(permission -> BotPermissionUtil.canManage(parentChannel, permission))
                .collect(Collectors.toList());

        if (memberOverride) {
            return channelAction.addMemberPermissionOverride(id, allowList, denyList);
        } else {
            return channelAction.addRolePermissionOverride(id, allowList, denyList);
        }
    }

    public static IPermissionContainerManager<?, ?> addPermission(IPermissionContainer parentChannel, IPermissionContainerManager<?, ?> channelManager,
                                                                  IPermissionHolder permissionHolder, boolean allow, Permission... permissions
    ) {
        return addPermission(parentChannel, channelManager, parentChannel.getPermissionOverride(permissionHolder), allow,
                permissionHolder instanceof Member, permissionHolder.getIdLong(), permissions);
    }

    public static IPermissionContainerManager<?, ?> addPermission(IPermissionContainer parentChannel, IPermissionContainerManager<?, ?> channelManager,
                                                                          PermissionOverride permissionOverride, boolean allow, Permission... permissions
    ) {
        return addPermission(parentChannel, channelManager, permissionOverride, allow,
                permissionOverride.isMemberOverride(), permissionOverride.getIdLong(), permissions);
    }

    private static IPermissionContainerManager<?, ?> addPermission(IPermissionContainer parentChannel, IPermissionContainerManager<?, ?> channelManager,
                                                                         PermissionOverride permissionOverride, boolean allow, boolean memberOverride,
                                                                         long id, Permission... permissions
    ) {
        long allowRaw = 0L;
        long denyRaw = 0L;

        if (permissionOverride != null) {
            allowRaw |= permissionOverride.getAllowedRaw();
            denyRaw |= permissionOverride.getDeniedRaw();
        }

        if (allow) {
            allowRaw |= Permission.getRaw(permissions);
            denyRaw &= ~Permission.getRaw(permissions);
        } else {
            allowRaw &= ~Permission.getRaw(permissions);
            denyRaw |= Permission.getRaw(permissions);
        }

        List<Permission> allowList = Permission.getPermissions(allowRaw).stream()
                .filter(permission -> BotPermissionUtil.canManage(parentChannel, permission))
                .collect(Collectors.toList());

        List<Permission> denyList = Permission.getPermissions(denyRaw).stream()
                .filter(permission -> BotPermissionUtil.canManage(parentChannel, permission))
                .collect(Collectors.toList());

        if (memberOverride) {
            return channelManager.putMemberPermissionOverride(id, allowList, denyList);
        } else {
            return channelManager.putRolePermissionOverride(id, allowList, denyList);
        }
    }

    public static <T extends GuildChannel> ChannelAction<T> clearPermissionOverrides(ChannelAction<T> channelAction) {
        channelAction = channelAction.clearPermissionOverrides();
        return channelAction.addPermissionOverride(channelAction.getGuild().getPublicRole(), 0L, 0L);
    }

}
