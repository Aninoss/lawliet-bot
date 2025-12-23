package core.utils;

import commands.Command;
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

    public static MessageComponentTree createCommandComponentTree(Command command, Collection<? extends ContainerChildComponent> components) {
        return createCommandComponentTree(command, components, Separator.Spacing.SMALL);
    }

    public static MessageComponentTree createCommandComponentTree(Command command, ContainerChildComponent component, Separator.Spacing spacing) {
        return createCommandComponentTree(command, List.of(component), spacing);
    }

    public static MessageComponentTree createCommandComponentTree(Command command, Collection<? extends ContainerChildComponent> components, Separator.Spacing spacing) {
        ArrayList<ContainerChildComponent> innerComponents = new ArrayList<>();
        innerComponents.add(TextDisplay.of("### " + command.getCommandProperties().emoji() + " " + command.getCommandLanguage().getTitle()));
        command.getUsername().ifPresent(username ->
                innerComponents.add(TextDisplay.of("-# @" + StringUtil.escapeMarkdown(username)))
        );
        innerComponents.add(Separator.createDivider(spacing));
        innerComponents.addAll(components);

        return MessageComponentTree.of(
                Container.of(innerComponents)
                        .withAccentColor(DEFAULT_CONTAINER_COLOR)
        );
    }

}
