package events.discordevents.guildmemberroleremove;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRoleRemoveAbstract;
import mysql.modules.stickyroles.DBStickyRoles;
import mysql.modules.stickyroles.StickyRolesData;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildMemberRoleRemoveStickyRoles extends GuildMemberRoleRemoveAbstract {

    @Override
    public boolean onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) throws Throwable {
        StickyRolesData stickyRolesData = DBStickyRoles.getInstance().retrieve(event.getGuild().getIdLong());
        stickyRolesData.getActions().removeIf(
                actionData -> event.getUser().getIdLong() == actionData.getMemberId() &&
                        event.getRoles().stream().anyMatch(role -> role.getIdLong() == actionData.getRoleId())
        );

        return true;
    }

}
