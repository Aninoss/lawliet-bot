package commands.stateprocessor;

import commands.runnables.NavigationAbstract;
import core.TextManager;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import org.glassfish.jersey.internal.util.Producer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RolesStateProcessor extends AbstractStateProcessor<List<Long>> {

    public static final String SELECT_MENU_ID = "entities";

    private final int min;
    private final int max;
    private final boolean checkAccess;
    private final Producer<List<Long>> getter;

    public RolesStateProcessor(NavigationAbstract command, int state, int stateBack, String propertyName, int min, int max, boolean checkAccess, Producer<List<Long>> getter, Consumer<List<Long>> setter) {
        this(command, state, stateBack, propertyName, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_roles_desc"), min, max, checkAccess, getter, setter);
    }

    public RolesStateProcessor(NavigationAbstract command, int state, int stateBack, String propertyName, String description, int min, int max, boolean checkAccess, Producer<List<Long>> getter, Consumer<List<Long>> setter) {
        super(command, state, stateBack, propertyName, description, false, setter);
        this.min = min;
        this.max = max;
        this.checkAccess = checkAccess;
        this.getter = getter;
    }

    @Override
    public boolean controllerEntitySelectMenu(EntitySelectInteractionEvent event) {
        List<Role> roles = event.getMentions().getRoles();
        if (checkAccess && !getCommand().checkRolesWithLog(event.getMember(), roles, true)) {
            return true;
        }

        set(roles.stream().map(ISnowflake::getIdLong).collect(Collectors.toList()));
        return true;
    }

    @Override
    protected void addActionRows(ArrayList<ActionRow> actionRows) {
        List<EntitySelectMenu.DefaultValue> defaultValues = getter.call().stream().map(EntitySelectMenu.DefaultValue::role).collect(Collectors.toList());
        EntitySelectMenu entitySelectMenu = EntitySelectMenu.create(SELECT_MENU_ID, EntitySelectMenu.SelectTarget.ROLE)
                .setDefaultValues(defaultValues)
                .setRequiredRange(min, max)
                .build();
        actionRows.add(ActionRow.of(entitySelectMenu));
    }

}