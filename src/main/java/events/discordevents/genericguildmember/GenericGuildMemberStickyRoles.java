package events.discordevents.genericguildmember;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GenericGuildMemberAbstract;
import modules.StickyRoles;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent;

import java.time.Instant;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GenericGuildMemberStickyRoles extends GenericGuildMemberAbstract {

    @Override
    public boolean onGuildMember(GenericGuildMemberEvent event, EntityManagerWrapper entityManager) {
        if (event.getMember().getTimeJoined().toInstant().isBefore(Instant.now().minusSeconds(10))) {
            GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
            StickyRoles.updateFromMemberRoles(guildEntity, event.getMember());
        }
        return true;
    }

}
