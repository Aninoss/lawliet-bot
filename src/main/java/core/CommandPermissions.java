package core;

import commands.Category;
import commands.Command;
import commands.CommandContainer;
import core.utils.BotPermissionUtil;
import core.utils.JDAUtil;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.guild.SlashPermissionEntity;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.commands.privileges.IntegrationPrivilege;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CommandPermissions {

    public static boolean transferCommandPermissions(Guild guild, GuildEntity guildEntity) {
        guildEntity.getSlashPermissions().clear();

        Map<String, List<IntegrationPrivilege>> externalMap = guild.retrieveCommandPrivileges().complete().getAsMap();
        for (String commandId : externalMap.keySet()) {
            String commandName = commandId.equals(guild.getSelfMember().getId())
                    ? ""
                    : SlashAssociations.getNameFromId(Long.parseLong(commandId));
            if (commandName == null) {
                continue;
            }

            for (IntegrationPrivilege integrationPrivilege : externalMap.get(commandId)) {
                SlashPermissionEntity entity = new SlashPermissionEntity();
                entity.setCommand(commandName);
                entity.setObjectId(integrationPrivilege.getIdLong());
                entity.setType(mapPermissionType(integrationPrivilege.getType()));
                entity.setEnabled(integrationPrivilege.isEnabled());

                if (entity.getType() != SlashPermissionEntity.Type.UNKNOWN) {
                    guildEntity.getSlashPermissions().add(entity);
                }
            }
        }

        return true;
    }

    public static boolean hasAccess(GuildEntity guildEntity, Category category, Member member, GuildChannel channel, boolean ignoreAdmin) {
        if (CommandContainer.getCommandCategoryMap().containsKey(category)) {
            return CommandContainer.getCommandCategoryMap().get(category).stream()
                    .anyMatch(clazz -> hasAccess(guildEntity, clazz, member, channel, ignoreAdmin));
        } else {
            return true;
        }
    }

    public static boolean hasAccess(GuildEntity guildEntity, Class<? extends Command> clazz, Member member, GuildChannel channel, boolean ignoreAdmin) {
        if (!ignoreAdmin && (BotPermissionUtil.can(member, Permission.ADMINISTRATOR) || member.isOwner())) {
            return true;
        }

        ArrayList<SlashPermissionEntity> commandSlashPermissions = new ArrayList<>();
        ArrayList<SlashPermissionEntity> generalSlashPermissions = new ArrayList<>();

        String commandName = SlashAssociations.findName(clazz);
        for (SlashPermissionEntity slashPermission : guildEntity.getSlashPermissions()) {
            if (commandName != null && slashPermission.getCommand().equals(commandName)) {
                commandSlashPermissions.add(slashPermission);
            } else if (slashPermission.getCommand().isEmpty()) {
                generalSlashPermissions.add(slashPermission);
            }
        }

        if (!commandSlashPermissions.isEmpty()) {
            return checkCommandAccess(commandSlashPermissions, member, channel);
        } else if (!generalSlashPermissions.isEmpty()) {
            return checkCommandAccess(generalSlashPermissions, member, channel);
        } else {
            return true;
        }
    }

    private static boolean checkCommandAccess(List<SlashPermissionEntity> slashPermissions, Member member, GuildChannel channel) {
        return checkPermissionsRolesAndUsers(slashPermissions, member) &&
                (channel == null || checkPermissionsChannels(slashPermissions, channel));
    }

    private static boolean checkPermissionsRolesAndUsers(List<SlashPermissionEntity> commandPermissions, Member member) {
        Boolean allowed = null;
        for (SlashPermissionEntity commandPermission : commandPermissions) {
            if (commandPermission.getType() == SlashPermissionEntity.Type.USER && commandPermission.getObjectId() == member.getIdLong()) {
                return commandPermission.getEnabled();
            }
            if (commandPermission.getType() == SlashPermissionEntity.Type.ROLE) {
                if (commandPermission.isDefaultObject(member.getGuild().getIdLong())) {
                    if (allowed == null && !commandPermission.getEnabled()) {
                        allowed = false;
                    }
                } else if (member.getRoles().stream().anyMatch(r -> r.getIdLong() == commandPermission.getObjectId())) {
                    if (commandPermission.getEnabled()) {
                        allowed = true;
                    } else if (allowed == null) {
                        allowed = false;
                    }
                }
            }
        }
        return Objects.requireNonNullElse(allowed, true);
    }

    private static boolean checkPermissionsChannels(List<SlashPermissionEntity> commandPermissions, GuildChannel channel) {
        boolean allowed = true;
        for (SlashPermissionEntity commandPermission : commandPermissions) {
            if (commandPermission.getType() == SlashPermissionEntity.Type.CHANNEL) {
                if (commandPermission.isDefaultObject(channel.getGuild().getIdLong())) {
                    allowed = commandPermission.getEnabled();
                } else {
                    if (JDAUtil.channelOrParentEqualsId(channel, commandPermission.getObjectId())) {
                        return commandPermission.getEnabled();
                    }
                }
            }
        }
        return allowed;
    }

    private static SlashPermissionEntity.Type mapPermissionType(IntegrationPrivilege.Type type) {
        return switch (type) {
            case ROLE -> SlashPermissionEntity.Type.ROLE;
            case USER -> SlashPermissionEntity.Type.USER;
            case CHANNEL -> SlashPermissionEntity.Type.CHANNEL;
            default -> SlashPermissionEntity.Type.UNKNOWN;
        };
    }

}
