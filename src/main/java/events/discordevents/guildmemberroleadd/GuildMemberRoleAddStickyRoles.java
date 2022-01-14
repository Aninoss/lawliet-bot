package events.discordevents.guildmemberroleadd;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRoleAddAbstract;
import mysql.modules.stickyroles.DBStickyRoles;
import mysql.modules.stickyroles.StickyRolesActionData;
import mysql.modules.stickyroles.StickyRolesData;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildMemberRoleAddStickyRoles extends GuildMemberRoleAddAbstract {

    @Override
    public boolean onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) throws Throwable {
        StickyRolesData stickyRolesData = DBStickyRoles.getInstance().retrieve(event.getGuild().getIdLong());
        stickyRolesData.getRoleIds().stream()
                .filter(stickyRoleId -> event.getRoles().stream().anyMatch(role -> role.getIdLong() == stickyRoleId))
                .forEach(stickyRoleId -> {
                    stickyRolesData.getActions().add(new StickyRolesActionData(
                            event.getGuild().getIdLong(),
                            event.getUser().getIdLong(),
                            stickyRoleId
                    ));
                });

        return true;
    }

}
