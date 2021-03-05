package core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Command;
import core.utils.BotPermissionUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import javafx.util.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Role;
import java.time.Duration;
import java.util.*;

public class PermissionCheckRuntime {

    private static final PermissionCheckRuntime instance = new PermissionCheckRuntime();
    private static final long PERMISSION_ROLE_POS = -1;

    private final Cache<Pair<Long, Long>, Boolean> errorCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(6))
            .build();

    private PermissionCheckRuntime() {}
    public static PermissionCheckRuntime getInstance() {
        return instance;
    }

    public boolean botHasPermission(Locale locale, Class<? extends Command> c, Guild guild, Permission... permissions) {
        return botHasPermission(locale, c, guild, null, permissions);
    }

    public boolean botHasPermission(Locale locale, Class<? extends Command> c, GuildChannel channel, Permission... permissions) {
        return botHasPermission(locale, c, channel.getGuild(), channel, permissions);
    }

    private boolean botHasPermission(Locale locale, Class<? extends Command> c, Guild guild, GuildChannel channel, Permission... permissions) {
        List<Permission> missingPermissions;
        if (channel != null) {
            missingPermissions = BotPermissionUtil.getMissingPermissions(channel, guild.getSelfMember(), permissions);
        } else {
            missingPermissions = BotPermissionUtil.getMissingPermissions(guild.getSelfMember(), permissions);
        }

        if (missingPermissions.size() == 0){
            return true;
        }

        if (canPostError(guild, Permission.getRaw(permissions)) && canContactOwner(guild)) {
            String permissionsList = new ListGen<Permission>().getList(missingPermissions, ListGen.SLOT_TYPE_BULLET, permission -> "**" + TextManager.getString(locale, TextManager.PERMISSIONS, permission.getName()) + "**");
            EmbedBuilder eb = EmbedFactory.getEmbedError();
            eb.setTitle(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_title"));
            eb.setDescription(TextManager.getString(locale, TextManager.GENERAL, "permission_runtime", channel != null, Command.getCommandProperties(c).trigger(), channel != null ? (channel.getType() == ChannelType.TEXT ? "#" : "") + StringUtil.escapeMarkdown(channel.getName()) : "", permissionsList));

            Optional.ofNullable(guild.getOwner()).ifPresent(owner -> JDAUtil.sendPrivateMessage(owner, eb.build()).queue());
            setErrorInstant(guild, Permission.getRaw(permissions));
        }

        return false;
    }

    public boolean botCanManageRoles(Locale locale, Class<? extends Command> c, List<Role> roles) {
        return botCanManageRoles(locale, c, roles.toArray(new Role[0]));
    }

    public boolean botCanManageRoles(Locale locale, Class<? extends Command> c, Role... roles) {
        ArrayList<Role> unreachableRoles = new ArrayList<>();

        for(Role role: roles) {
            if (!role.getGuild().getSelfMember().canInteract(role)) {
                unreachableRoles.add(role);
            }
        }

        if (unreachableRoles.size() == 0) {
            return true;
        }

        Guild guild = roles[0].getGuild();
        if (guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES) && canPostError(guild, PERMISSION_ROLE_POS) && canContactOwner(guild)) {
            String rolesList = new ListGen<Role>().getList(unreachableRoles, ListGen.SLOT_TYPE_BULLET, role -> "**@" + StringUtil.escapeMarkdown(role.getName()) + "**");
            EmbedBuilder eb = EmbedFactory.getEmbedError();
            eb.setTitle(TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_title"));
            eb.setDescription(TextManager.getString(locale, TextManager.GENERAL, "permission_runtime_rolespos", Command.getCommandProperties(c).trigger(), rolesList));

            Optional.ofNullable(guild.getOwner()).ifPresent(owner -> JDAUtil.sendPrivateMessage(owner, eb.build()).queue());
            setErrorInstant(guild, PERMISSION_ROLE_POS);
        }

        return false;
    }

    private boolean canContactOwner(Guild guild) {
        return canPostError(guild, Permission.getRaw(Permission.MANAGE_ROLES)) && guild.getOwner() != null;
    }

    private boolean canPostError(Guild guild, long permissionsRaw) {
        return errorCache.asMap().containsKey(new Pair<>(guild.getIdLong(), permissionsRaw));
    }

    private void setErrorInstant(Guild guild, long permissionsRaw) {
        errorCache.put(new Pair<>(guild.getIdLong(), permissionsRaw), true);
    }

}
