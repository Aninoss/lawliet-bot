package events.discordevents.guildmemberremove;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRemoveAbstract;
import modules.StickyRoles;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;

@DiscordEvent
public class GuildMemberRemoveStickyRoles extends GuildMemberRemoveAbstract {

    @Override
    public boolean onGuildMemberRemove(GuildMemberRemoveEvent event, EntityManagerWrapper entityManager) {
        Member member = event.getMember();
        if (member != null) {
            StickyRoles.updateFromMemberRoles(member, true, true);
        }
        return true;
    }

}
