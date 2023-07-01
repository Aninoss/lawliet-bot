package events.discordevents.guildupdateboostcount;

import java.util.Locale;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildUpdateBoostCountAbstract;
import modules.MemberCountDisplay;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateBoostCountEvent;

@DiscordEvent
public class GuildUpdateBoostCountMCDisplays extends GuildUpdateBoostCountAbstract {

    @Override
    public boolean onGuildUpdateBoostCount(GuildUpdateBoostCountEvent event, EntityManagerWrapper entityManager) {
        Locale locale = entityManager.findGuildEntity(event.getGuild().getIdLong()).getLocale();
        MemberCountDisplay.manage(locale, event.getGuild());
        return true;
    }

}
