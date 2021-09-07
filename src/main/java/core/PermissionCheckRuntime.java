package core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Command;
import core.utils.BotPermissionUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import javafx.util.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Role;

public class PermissionCheckRuntime {

    private static final long PERMISSION_ROLE_POS = -1;

    private static final Cache<Pair<Long, Long>, Boolean> errorCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(6))
            .build();

    public static boolean botHasPermission(Locale locale, Class<? extends Command> c, Guild guild, Permission... permissions) {
        return botHasPermission(locale, c, guild, null, permissions);
    }

    public static boolean botHasPermission(Locale locale, Class<? extends Command> c, GuildChannel channel, Permission... permissions) {
        if (channel == null) {
            return true;
        }
        return botHasPermission(locale, c, channel.getGuild(), channel, permissions);
    }

    private static boolean botHasPermission(Locale locale, Class<? extends Command> c, Guild guild, GuildChannel channel, Permission... permissions) {
        if (guild == null && channel == null) {
            return true;
        }

        long id;
        List<Permission> missingPermissions;
        if (channel != null) {
            id = channel.getIdLong();
            missingPermissions = BotPermissionUtil.getMissingPermissions(channel, guild.getSelfMember(), permissions);
        } else {
            id = guild.getIdLong();
            missingPermissions = BotPermissionUtil.getMissingPermissions(guild.getSelfMember(), permissions);
        }

        if (missingPermissions.size() == 0) {
            return true;
        }

        if (canPostError(id, Permission.getRaw(permissions)) && guild.getOwner() != null) {
            String permissionsList = new ListGen<Permission>().getList(missingPermissions, ListGen.SLOT_TYPE_BULLET, permission -> "**" + TextManager.getString(locale, TextManager.PERMISSIONS, permission.name()) + "**");
            EmbedBuilder eb = EmbedFactory.getEmbedError();
            eb.setTitle(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_title"));
            eb.setDescription(TextManager.getString(locale, TextManager.GENERAL, "permission_runtime", channel != null, Command.getCommandProperties(c).trigger(), channel != null ? channel.getAsMention() : "", permissionsList));

            Optional.ofNullable(guild.getOwner()).ifPresent(owner -> JDAUtil.sendPrivateMessage(owner, eb.build()).queue());
            setErrorInstant(id, Permission.getRaw(permissions));
        }

        return false;
    }

    public static boolean botCanManageRoles(Locale locale, Class<? extends Command> c, List<Role> roles) {
        return botCanManageRoles(locale, c, roles.toArray(new Role[0]));
    }

    public static boolean botCanManageRoles(Locale locale, Class<? extends Command> c, Role... roles) {
        ArrayList<Role> unreachableRoles = new ArrayList<>();

        for (Role role : roles) {
            if (!role.getGuild().getSelfMember().hasPermission(Permission.MANAGE_ROLES) || !BotPermissionUtil.canManage(role)) {
                unreachableRoles.add(role);
            }
        }

        if (unreachableRoles.size() == 0) {
            return true;
        }

        Guild guild = roles[0].getGuild();
        if (BotPermissionUtil.can(guild, Permission.MANAGE_ROLES) && canPostError(guild.getIdLong(), PERMISSION_ROLE_POS) && guild.getOwner() != null) {
            String rolesList = new ListGen<Role>().getList(unreachableRoles, ListGen.SLOT_TYPE_BULLET, role -> "**@" + StringUtil.escapeMarkdown(role.getName()) + "**");
            EmbedBuilder eb = EmbedFactory.getEmbedError();
            eb.setTitle(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_title"));
            eb.setDescription(TextManager.getString(locale, TextManager.GENERAL, "permission_runtime_rolespos", Command.getCommandProperties(c).trigger(), rolesList));

            Optional.ofNullable(guild.getOwner()).ifPresent(owner -> JDAUtil.sendPrivateMessage(owner, eb.build()).queue());
            setErrorInstant(guild.getIdLong(), PERMISSION_ROLE_POS);
        }

        return false;
    }

    private static boolean canPostError(long id, long permissionsRaw) {
        return !errorCache.asMap().containsKey(new Pair<>(id, permissionsRaw));
    }

    private static void setErrorInstant(long id, long permissionsRaw) {
        errorCache.put(new Pair<>(id, permissionsRaw), true);
    }

}
