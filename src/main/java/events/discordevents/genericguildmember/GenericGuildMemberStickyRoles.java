package events.discordevents.genericguildmember;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GenericGuildMemberAbstract;
import modules.StickyRoles;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.GuildEntity;
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GenericGuildMemberStickyRoles extends GenericGuildMemberAbstract {

    @Override
    public boolean onGuildMember(GenericGuildMemberEvent event, EntityManagerWrapper entityManager) {
        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        StickyRoles.updateFromMemberRoles(guildEntity, event.getMember());
        return true;
    }

}
