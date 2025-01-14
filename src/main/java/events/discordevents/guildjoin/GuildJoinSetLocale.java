package events.discordevents.guildjoin;

import constants.Language;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildJoinAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;

@DiscordEvent(priority = EventPriority.HIGH)
public class GuildJoinSetLocale extends GuildJoinAbstract {

    @Override
    public boolean onGuildJoin(GuildJoinEvent event, EntityManagerWrapper entityManager) {
        Language language = Language.from(event.getGuild().getLocale());
        if (language != null && language != Language.EN) {
            GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
            guildEntity.beginTransaction();
            guildEntity.setLanguage(language);
            guildEntity.commitTransaction();
        }

        return true;
    }

}
