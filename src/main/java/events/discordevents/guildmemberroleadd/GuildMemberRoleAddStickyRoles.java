package events.discordevents.guildmemberroleadd;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRoleAddAbstract;
import modules.StickyRoles;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildMemberRoleAddStickyRoles extends GuildMemberRoleAddAbstract {

    @Override
    public boolean onGuildMemberRoleAdd(GuildMemberRoleAddEvent event, EntityManagerWrapper entityManager) throws Throwable {
        StickyRoles.updateFromMemberRoles(event.getMember(), true, false);
        return true;
    }

}
