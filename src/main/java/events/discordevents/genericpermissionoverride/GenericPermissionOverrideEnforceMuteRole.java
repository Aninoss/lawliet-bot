package events.discordevents.genericpermissionoverride;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GenericPermissionOverrideAbstract;
import modules.Mute;
import mysql.modules.moderation.DBModeration;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.events.guild.override.GenericPermissionOverrideEvent;

@DiscordEvent
public class GenericPermissionOverrideEnforceMuteRole extends GenericPermissionOverrideAbstract {

    @Override
    public boolean onGenericPermissionOverride(GenericPermissionOverrideEvent event) {
        PermissionOverride permissionOverride = event.getPermissionOverride();
        DBModeration.getInstance().retrieve(event.getGuild().getIdLong()).getMuteRole().ifPresent(muteRole -> {
            if (event.getPermissionOverride().getIdLong() == muteRole.getIdLong() &&
                    (!permissionOverride.getDenied().contains(Permission.MESSAGE_WRITE) || event.getChannel().getPermissionOverride(muteRole) == null)
            ) {
                Mute.enforceMuteRole(event.getGuild());
            }
        });
        return true;
    }

}