package events.discordevents.guildvoiceupdate;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildVoiceUpdateAbstract;
import modules.AutoChannel;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.GuildEntity;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;

@DiscordEvent
public class GuildVoiceUpdateAutoChannelCreate extends GuildVoiceUpdateAbstract {

    @Override
    public boolean onGuildVoiceUpdate(GuildVoiceUpdateEvent event, EntityManagerWrapper entityManager) {
        if (event.getChannelJoined() != null && event.getChannelJoined() instanceof VoiceChannel) {
            GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
            AutoChannel.processCreate((VoiceChannel) event.getChannelJoined(), event.getMember(), guildEntity);
        }
        return true;
    }

}
