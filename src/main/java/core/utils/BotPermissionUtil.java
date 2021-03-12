package core.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import core.EmbedFactory;
import core.TextManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

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

    public static List<Permission> getMissingPermissions(Member member, Permission... permissions) {
        permissions = Arrays.copyOf(permissions, permissions.length + 1);
        permissions[permissions.length - 1] = Permission.VIEW_CHANNEL;

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

    public static EmbedBuilder getUserPermissionMissingEmbed(Locale locale, List<Permission> userPermissions, List<Permission> botPermissions) {
        EmbedBuilder eb = null;
        if (userPermissions.size() != 0 || botPermissions.size() != 0) {
            eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_title"));

            if (userPermissions.size() > 0) {
                StringBuilder desc = new StringBuilder();
                for (Permission permission : userPermissions) {
                    desc.append("• ");
                    desc.append(TextManager.getString(locale, TextManager.PERMISSIONS, permission.name()));
                    desc.append("\n");
                }
                eb.addField(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_you"), desc.toString(), false);
            }

            if (botPermissions.size() > 0) {
                StringBuilder desc = new StringBuilder();
                for (Permission permission : botPermissions) {
                    desc.append("• ");
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

    public static boolean canRead(TextChannel channel, Permission... permissions) {
        return channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_HISTORY) &&
                can(channel, permissions);
    }

    public static boolean canWrite(TextChannel channel, Permission... permissions) {
        return channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_WRITE) &&
                can(channel, permissions);
    }

    public static boolean canWrite(Member member, TextChannel channel, Permission... permissions) {
        return member.hasPermission(channel, Permission.MESSAGE_WRITE) &&
                can(member, channel, permissions);
    }

    public static boolean canWriteEmbed(TextChannel channel, Permission... permissions) {
        return channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS) &&
                canWrite(channel, permissions);
    }

    public static boolean canInteract(Guild guild, User user) {
        return canInteract(guild, user.getIdLong());
    }

    public static boolean canInteract(Guild guild, long userId) {
        Member member = guild.getMemberById(userId);
        if (member != null) {
            return guild.getSelfMember().canInteract(member);
        } else {
            return true;
        }
    }

    public static boolean canInteract(Guild guild, Permission permission) {
        if (permission == Permission.MANAGE_PERMISSIONS || permission == Permission.MANAGE_ROLES) {
            return guild.getSelfMember().hasPermission(Permission.ADMINISTRATOR);
        } else {
            return guild.getSelfMember().hasPermission(permission);
        }
    }

    public static List<Role> getMemberRoles(Guild guild) {
        int min = (int) (guild.getMemberCount() * 0.75);
        return guild.getRoles().stream()
                .filter(role -> role.getGuild().getMembersWithRoles(role).size() >= min)
                .collect(Collectors.toList());
    }

    public static boolean channelIsPublic(GuildChannel channel) {
        Guild guild = channel.getGuild();
        return guild.getMembers().stream()
                .filter(member -> member.hasAccess(channel))
                .count() >= guild.getMemberCount() * 0.75;
    }

    public static boolean memberCanMentionRoles(TextChannel channel, Member member, String messageContent) {
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

}
