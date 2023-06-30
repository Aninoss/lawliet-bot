package events.discordevents.applicationupdateprivileges;

import core.CommandPermissions;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ApplicationUpdatePrivilegesAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.interaction.command.ApplicationUpdatePrivilegesEvent;

@DiscordEvent
public class ApplicationUpdatePrivilegesCommandPermissions extends ApplicationUpdatePrivilegesAbstract {

    @Override
    public boolean onApplicationUpdatePrivileges(ApplicationUpdatePrivilegesEvent event, EntityManagerWrapper entityManager) {
        CommandPermissions.transferCommandPermissions(event.getGuild());
        return true;
    }

}
