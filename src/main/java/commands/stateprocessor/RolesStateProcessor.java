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
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RolesStateProcessor extends AbstractStateProcessor<List<Long>, RolesStateProcessor> {

    public static final String SELECT_MENU_ID = "entities";

    private int min = 0;
    private int max = EntitySelectMenu.OPTIONS_MAX_AMOUNT;
    private boolean checkAccess = false;

    public RolesStateProcessor(NavigationAbstract command, int state, int stateBack, String propertyName) {
        super(command, state, stateBack, propertyName, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_roles_desc"));
        setGetter(Collections::emptyList);
    }

    public RolesStateProcessor setMinMax(int min, int max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public RolesStateProcessor setCheckAccess(boolean checkAccess) {
        this.checkAccess = checkAccess;
        return this;
    }

    public RolesStateProcessor setSingleGetter(Producer<Long> getter) {
        setGetter(() -> {
            Long value = getter.call();
            return value == null ? Collections.emptyList() : List.of(value);
        });
        return this;
    }

    public RolesStateProcessor setSingleSetter(Consumer<Long> setter) {
        setSetter(list -> setter.accept(list.isEmpty() ? null : list.get(0)));
        return this;
    }

    @Override
    public boolean controllerEntitySelectMenu(EntitySelectInteractionEvent event) {
        List<Role> roles = event.getMentions().getRoles();
        if (checkAccess && !getCommand().checkRolesWithLog(event.getMember(), roles, true)) {
            return true;
        }

        List<Long> newValues = roles.stream().map(ISnowflake::getIdLong).collect(Collectors.toList());
        set(newValues);
        return true;
    }

    @Override
    protected void addActionRows(ArrayList<ActionRow> actionRows) {
        List<Long> valueList = get();
        if (valueList == null) {
            valueList = Collections.emptyList();
        }

        List<EntitySelectMenu.DefaultValue> defaultValues = valueList.stream().map(EntitySelectMenu.DefaultValue::role).collect(Collectors.toList());
        EntitySelectMenu entitySelectMenu = EntitySelectMenu.create(SELECT_MENU_ID, EntitySelectMenu.SelectTarget.ROLE)
                .setDefaultValues(defaultValues.stream().limit(max).collect(Collectors.toList()))
                .setRequiredRange(min, max)
                .build();
        actionRows.add(ActionRow.of(entitySelectMenu));
    }

}
