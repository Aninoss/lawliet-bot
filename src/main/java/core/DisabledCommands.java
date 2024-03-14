package core;

import commands.Category;
import commands.Command;
import commands.CommandContainer;
import core.utils.BotPermissionUtil;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DisabledCommands {

    public static Set<Category> getDisabledCommandCategories(GuildEntity guildEntity) {
        return guildEntity.getDisabledCommandsAndCategories().stream()
                .map(Category::fromId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public static Set<String> getDisabledCommands(GuildEntity guildEntity) {
        return guildEntity.getDisabledCommandsAndCategories().stream()
                .filter(element -> CommandContainer.getCommandMap().get(element) != null)
                .collect(Collectors.toSet());
    }

    public static boolean commandIsEnabled(GuildEntity guildEntity, Command command) {
        Set<String> disabledCommandsAndCategories = guildEntity.getDisabledCommandsAndCategories();
        return !disabledCommandsAndCategories.contains(command.getTrigger()) &&
                !disabledCommandsAndCategories.contains(command.getCategory().getId());
    }

    public static boolean commandIsEnabledEffectively(GuildEntity guildEntity, Command command, Member member) {
        return BotPermissionUtil.can(member, Permission.ADMINISTRATOR) ||
                commandIsEnabled(guildEntity, command);
    }

    public static boolean commandCategoryIsEnabled(GuildEntity guildEntity, Category category) {
        return !guildEntity.getDisabledCommandsAndCategories().contains(category.getId());
    }

    public static boolean commandCategoryIsEnabledEffectively(GuildEntity guildEntity, Category category, Member member) {
        return BotPermissionUtil.can(member, Permission.ADMINISTRATOR) ||
                commandCategoryIsEnabled(guildEntity, category);
    }

    public static boolean elementIsEnabled(GuildEntity guildEntity, String element) {
        return !guildEntity.getDisabledCommandsAndCategories().contains(element);
    }

    public static boolean elementIsEnabledEffectively(GuildEntity guildEntity, String element, Member member) {
        return BotPermissionUtil.can(member, Permission.ADMINISTRATOR) ||
                elementIsEnabled(guildEntity, element);
    }

}
