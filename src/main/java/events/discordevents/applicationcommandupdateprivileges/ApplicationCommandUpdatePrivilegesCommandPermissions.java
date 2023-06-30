package events.discordevents.applicationcommandupdateprivileges;

import core.CommandPermissions;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ApplicationCommandUpdatePrivilegesAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.interaction.command.ApplicationCommandUpdatePrivilegesEvent;

@DiscordEvent
public class ApplicationCommandUpdatePrivilegesCommandPermissions extends ApplicationCommandUpdatePrivilegesAbstract {

    @Override
    public boolean onApplicationCommandUpdatePrivileges(ApplicationCommandUpdatePrivilegesEvent event, EntityManagerWrapper entityManager) {
        CommandPermissions.transferCommandPermissions(event.getGuild());
        return true;
    }

}
