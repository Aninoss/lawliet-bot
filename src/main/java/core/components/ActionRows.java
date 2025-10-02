package core.components;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;

import java.util.Collections;
import java.util.List;

public class ActionRows {

    public static List<ActionRow> of(ActionRowChildComponent... components) {
        return of(List.of(components));
    }

    public static List<ActionRow> of(List<? extends ActionRowChildComponent> components) {
        if (components == null || components.isEmpty()) {
            return Collections.emptyList();
        }
        return ActionRow.partitionOf(components);
    }

}
