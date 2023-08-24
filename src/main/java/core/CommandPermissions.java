package core;

import commands.Category;
import commands.Command;
import commands.CommandContainer;
import core.utils.BotPermissionUtil;
import mysql.modules.slashpermissions.DBSlashPermissions;
import mysql.modules.slashpermissions.SlashPermissionsSlot;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.privileges.IntegrationPrivilege;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CommandPermissions {

    public static boolean transferCommandPermissions(Guild guild) {
        Map<String, List<IntegrationPrivilege>> externalMap = guild.retrieveCommandPrivileges().complete().getAsMap();
        HashMap<String, List<SlashPermissionsSlot>> internalMap = new HashMap<>();

        for (String commandId : externalMap.keySet()) {
            String commandName = commandId.equals(guild.getSelfMember().getId())
                    ? ""
                    : SlashAssociations.getNameFromId(Long.parseLong(commandId));
            if (commandName == null) {
                continue;
            }

            List<SlashPermissionsSlot> internalList = externalMap.get(commandId).stream()
                    .map(e -> new SlashPermissionsSlot(
                            guild.getIdLong(),
                            commandName,
                            e.getIdLong(),
                            mapPermissionType(e.getType()),
                            e.isEnabled()
                    ))
                    .filter(e -> e.getType() != null)
                    .collect(Collectors.toList());
            internalMap.put(commandName, internalList);
        }

        DBSlashPermissions.getInstance().retrieve(guild.getIdLong()).setPermissionMap(internalMap);
        return true;
    }

    public static boolean hasAccess(Category category, Member member, TextChannel textChannel, boolean ignoreAdmin) {
        if (CommandContainer.getCommandCategoryMap().containsKey(category)) {
            return CommandContainer.getCommandCategoryMap().get(category).stream()
                    .anyMatch(clazz -> hasAccess(clazz, member, textChannel, ignoreAdmin));
        } else {
            return true;
        }
    }

    public static boolean hasAccess(Class<? extends Command> clazz, Member member, TextChannel textChannel, boolean ignoreAdmin) {
        if (!ignoreAdmin && (BotPermissionUtil.can(member, Permission.ADMINISTRATOR) || member.isOwner())) {
            return true;
        }

        Map<String, List<SlashPermissionsSlot>> permissionMap = DBSlashPermissions.getInstance().retrieve(member.getGuild().getIdLong())
                .getPermissionMap();
        String commandName = SlashAssociations.findName(clazz);
        if (commandName != null && permissionMap.containsKey(commandName)) {
            return checkCommandAccess(permissionMap.get(commandName), member, textChannel);
        } else if (permissionMap.containsKey("")) {
            return checkCommandAccess(permissionMap.get(""), member, textChannel);
        } else {
            return true;
        }
    }

    private static boolean checkCommandAccess(List<SlashPermissionsSlot> commandPermissions, Member member, TextChannel textChannel) {
        return checkPermissionsRolesAndUsers(commandPermissions, member) &&
                (textChannel == null || checkPermissionsChannels(commandPermissions, textChannel));
    }

    private static boolean checkPermissionsRolesAndUsers(List<SlashPermissionsSlot> commandPermissions, Member member) {
        Boolean allowed = null;
        for (SlashPermissionsSlot commandPermission : commandPermissions) {
            if (commandPermission.getType() == SlashPermissionsSlot.Type.USER && commandPermission.getObjectId() == member.getIdLong()) {
                return commandPermission.isAllowed();
            }
            if (commandPermission.getType() == SlashPermissionsSlot.Type.ROLE) {
                if (commandPermission.isDefaultObject()) {
                    if (allowed == null && !commandPermission.isAllowed()) {
                        allowed = false;
                    }
                } else if (member.getRoles().stream().anyMatch(r -> r.getIdLong() == commandPermission.getRoleId())) {
                    if (commandPermission.isAllowed()) {
                        allowed = true;
                    } else if (allowed == null) {
                        allowed = false;
                    }
                }
            }
        }
        return Objects.requireNonNullElse(allowed, true);
    }

    private static boolean checkPermissionsChannels(List<SlashPermissionsSlot> commandPermissions, TextChannel textChannel) {
        boolean allowed = true;
        for (SlashPermissionsSlot commandPermission : commandPermissions) {
            if (commandPermission.getType() == SlashPermissionsSlot.Type.CHANNEL) {
                if (commandPermission.isDefaultObject()) {
                    allowed = commandPermission.isAllowed();
                } else {
                    if (commandPermission.getStandardGuildMessageChannelId() == textChannel.getIdLong()) {
                        return commandPermission.isAllowed();
                    }
                }
            }
        }
        return allowed;
    }

    private static SlashPermissionsSlot.Type mapPermissionType(IntegrationPrivilege.Type type) {
        return switch (type) {
            case ROLE -> SlashPermissionsSlot.Type.ROLE;
            case USER -> SlashPermissionsSlot.Type.USER;
            case CHANNEL -> SlashPermissionsSlot.Type.CHANNEL;
            default -> null;
        };
    }

}
