package commands.stateprocessor;

import commands.runnables.NavigationAbstract;
import core.TextManager;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RolesStateProcessor extends AbstractLongListProcessor<RolesStateProcessor> {

    private boolean checkAccess = false;

    public RolesStateProcessor(NavigationAbstract command, int state, int stateBack, String propertyName) {
        super(command, state, stateBack, propertyName, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_roles_desc"),
                EntitySelectMenu.SelectTarget.ROLE, EntitySelectMenu.DefaultValue::role);
        setGetter(Collections::emptyList);
    }

    public RolesStateProcessor setCheckAccess(boolean checkAccess) {
        this.checkAccess = checkAccess;
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

}
