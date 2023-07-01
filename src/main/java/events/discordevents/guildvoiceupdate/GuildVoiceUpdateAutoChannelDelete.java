package events.discordevents.guildvoiceupdate;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildVoiceUpdateAbstract;
import modules.AutoChannel;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.GuildEntity;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;

@DiscordEvent
public class GuildVoiceUpdateAutoChannelDelete extends GuildVoiceUpdateAbstract {

    @Override
    public boolean onGuildVoiceUpdate(GuildVoiceUpdateEvent event, EntityManagerWrapper entityManager) {
        if (event.getChannelLeft() != null && event.getChannelLeft() instanceof VoiceChannel) {
            GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
            AutoChannel.processRemove((VoiceChannel) event.getChannelLeft(), guildEntity);
        }
        return true;
    }

}
