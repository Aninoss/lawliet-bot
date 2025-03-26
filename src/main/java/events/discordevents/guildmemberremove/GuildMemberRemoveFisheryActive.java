package events.discordevents.guildmemberremove;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRemoveAbstract;
import modules.fishery.FisheryStatus;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.redis.RedisManager;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import redis.clients.jedis.Pipeline;

@DiscordEvent
public class GuildMemberRemoveFisheryActive extends GuildMemberRemoveAbstract {

    @Override
    public boolean onGuildMemberRemove(GuildMemberRemoveEvent event, EntityManagerWrapper entityManager) {
        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        if (guildEntity.getFishery().getFisheryStatus() != FisheryStatus.STOPPED) {
            RedisManager.update(jedis -> {
                Pipeline pipeline = jedis.pipelined();
                FisheryUserManager.deleteUserActiveOnGuild(pipeline, event.getGuild().getIdLong(), event.getUser().getIdLong());
                pipeline.sync();
            });
        }

        return true;
    }

}
