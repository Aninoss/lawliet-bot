package core.utils;

import commands.Command;
import commands.runnables.configurationcategory.AlertsCommand;
import constants.LogStatus;
import core.TextManager;
import mysql.modules.tracker.DBTracker;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.MessageTopLevelComponentUnion;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import net.dv8tion.jda.api.entities.Member;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static java.util.Objects.requireNonNullElse;

public class ComponentsUtil {

    public static final Color DEFAULT_CONTAINER_COLOR = new Color(
            Integer.parseInt(requireNonNullElse(System.getenv("EMBED_R"), "255")),
            Integer.parseInt(requireNonNullElse(System.getenv("EMBED_G"), "255")),
            Integer.parseInt(requireNonNullElse(System.getenv("EMBED_B"), "255"))
    );
    public static final Color FAILED_CONTAINER_COLOR = Color.RED;

    public static MessageComponentTree createCommandComponentTree(Command command, ContainerChildComponent component) {
        return createCommandComponentTree(command, List.of(component));
    }

    public static MessageComponentTree createCommandComponentTree(Command command, Collection<ContainerChildComponent> components) {
        return createCommandComponentTree(command, components, DEFAULT_CONTAINER_COLOR);
    }

    public static MessageComponentTree createErrorNoArgs(Command command) {
        TextDisplay content = TextDisplay.of(TextManager.getString(command.getLocale(), TextManager.GENERAL, "no_args"));
        return createCommandComponentTreeError(command, content);
    }

    public static MessageComponentTree createErrorNsfwBlock(Command command) {
        TextDisplay content = TextDisplay.of(TextManager.getString(command.getLocale(), TextManager.GENERAL, "nsfw_block_description", command.getPrefix()));
        return createCommandComponentTreeError(command, content);
    }

    public static MessageComponentTree createErrorNoResults(Command command, String args) {
        TextDisplay content = TextDisplay.of(TextManager.getNoResultsString(command.getLocale(), args));
        return createCommandComponentTreeError(command, content);
    }

    public static MessageComponentTree createErrorApiDown(Command command, String service) {
        TextDisplay content = TextDisplay.of(TextManager.getString(command.getLocale(), TextManager.GENERAL, "api_down", service));
        return createCommandComponentTreeError(command, content);
    }

    public static MessageComponentTree createCommandComponentTreeError(Command command, ContainerChildComponent component) {
        return createCommandComponentTreeError(command, List.of(component));
    }

    public static MessageComponentTree createCommandComponentTreeError(Command command, Collection<ContainerChildComponent> components) {
        return createCommandComponentTree(command, components, FAILED_CONTAINER_COLOR);
    }

    public static MessageComponentTree createCommandComponentTree(Command command, Collection<ContainerChildComponent> components, Color accentColor) {
        ArrayList<ContainerChildComponent> innerComponents = new ArrayList<>();
        innerComponents.add(TextDisplay.of("### " + command.getCommandProperties().emoji() + " " + command.getCommandLanguage().getTitle()));
        innerComponents.add(Separator.createDivider(Separator.Spacing.SMALL));
        innerComponents.addAll(components);

        return MessageComponentTree.of(
                Container.of(innerComponents)
                        .withAccentColor(accentColor)
        );
    }

    public static MessageComponentTree addLog(MessageComponentTree componentTree, LogStatus logStatus, String log) {
        if (log == null || log.isEmpty()) {
            return componentTree;
        }

        String add = "";
        if (logStatus != null) {
            add = switch (logStatus) {
                case FAILURE -> "❌ ";
                case SUCCESS -> "✅ ";
                case WIN -> "🎉 ";
                case LOSE -> "☠️ ";
                case WARNING -> "⚠️️ ";
                case TIME -> "⏲️ ";
            };
        }

        ArrayList<MessageTopLevelComponent> newComponents = new ArrayList<>();
        for (MessageTopLevelComponentUnion component : componentTree.getComponents()) {
            if (component instanceof Container) {
                ArrayList<ContainerChildComponent> newContainerComponents = new ArrayList<>(((Container) component).getComponents());
                newContainerComponents.add(Separator.createInvisible(Separator.Spacing.SMALL));
                newContainerComponents.add(TextDisplay.of("> " + StringUtil.shortenString(add + log, 500)));
                newComponents.add(
                        Container.of(newContainerComponents)
                                .withAccentColor(((Container) component).getAccentColor())
                                .withSpoiler(((Container) component).isSpoiler())
                );
            } else {
                newComponents.add(component);
            }
        }

        return MessageComponentTree.of(newComponents);
    }

    public static MessageComponentTree addTrackerNoteLog(Command command, Member member, MessageComponentTree componentTree) {
        if (BotPermissionUtil.can(member, Command.getCommandProperties(AlertsCommand.class).userGuildPermissions()) &&
                DBTracker.getInstance().retrieve(member.getGuild().getIdLong()).values().stream().noneMatch(s -> s.getCommandTrigger().equals(command.getTrigger()))
        ) {
            return addLog(componentTree, LogStatus.WARNING, TextManager.getString(command.getLocale(), TextManager.GENERAL, "tracker", command.getPrefix(), command.getTrigger()));
        }
        return componentTree;
    }

    public static MessageComponentTree addTrackerRemoveLog(Locale locale, MessageComponentTree componentTree) {
        return addLog(componentTree, LogStatus.WARNING, TextManager.getString(locale, TextManager.GENERAL, "tracker_remove"));
    }

}
