package events.discordevents.applicationupdateprivileges;

import core.CommandPermissions;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ApplicationUpdatePrivilegesAbstract;
import net.dv8tion.jda.api.events.interaction.command.ApplicationUpdatePrivilegesEvent;

@DiscordEvent
public class ApplicationUpdatePrivilegesCommandPermissions extends ApplicationUpdatePrivilegesAbstract {

    @Override
    public boolean onApplicationUpdatePrivileges(ApplicationUpdatePrivilegesEvent event) {
        CommandPermissions.transferCommandPermissions(event.getGuild());
        return true;
    }

}
