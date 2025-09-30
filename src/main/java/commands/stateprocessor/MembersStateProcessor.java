package commands.stateprocessor;

import commands.runnables.NavigationAbstract;
import core.TextManager;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MembersStateProcessor extends AbstractLongListProcessor<MembersStateProcessor> {

    public MembersStateProcessor(NavigationAbstract command, int state, int stateBack, String propertyName) {
        super(command, state, stateBack, propertyName, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_members_desc"),
                EntitySelectMenu.SelectTarget.USER, EntitySelectMenu.DefaultValue::user);
        setGetter(Collections::emptyList);
    }

    @Override
    public boolean controllerEntitySelectMenu(EntitySelectInteractionEvent event) {
        List<Long> newValues = event.getMentions().getUsers().stream().map(ISnowflake::getIdLong).collect(Collectors.toList());
        set(newValues);
        return true;
    }

}
