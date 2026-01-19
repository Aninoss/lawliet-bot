package core.utils;

import commands.Command;
import constants.LogStatus;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.MessageTopLevelComponentUnion;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        ArrayList<ContainerChildComponent> innerComponents = new ArrayList<>();
        innerComponents.add(TextDisplay.of("### " + command.getCommandProperties().emoji() + " " + command.getCommandLanguage().getTitle()));
        command.getUsername().ifPresent(username ->
                innerComponents.add(TextDisplay.of("-# @" + StringUtil.escapeMarkdown(username)))
        );
        innerComponents.add(Separator.createDivider(Separator.Spacing.SMALL));
        innerComponents.addAll(components);

        return MessageComponentTree.of(
                Container.of(innerComponents)
                        .withAccentColor(DEFAULT_CONTAINER_COLOR)
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
                );
            } else {
                newComponents.add(component);
            }
        }

        return MessageComponentTree.of(newComponents);
    }

}
