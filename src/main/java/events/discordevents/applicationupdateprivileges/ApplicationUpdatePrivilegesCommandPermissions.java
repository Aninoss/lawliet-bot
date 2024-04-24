package events.discordevents.applicationupdateprivileges;

import core.CommandPermissions;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ApplicationUpdatePrivilegesAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.events.interaction.command.ApplicationUpdatePrivilegesEvent;

@DiscordEvent
public class ApplicationUpdatePrivilegesCommandPermissions extends ApplicationUpdatePrivilegesAbstract {

    @Override
    public boolean onApplicationUpdatePrivileges(ApplicationUpdatePrivilegesEvent event, EntityManagerWrapper entityManager) {
        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        guildEntity.beginTransaction();
        CommandPermissions.transferCommandPermissions(event.getGuild(), guildEntity);
        guildEntity.commitTransaction();
        return true;
    }

}
