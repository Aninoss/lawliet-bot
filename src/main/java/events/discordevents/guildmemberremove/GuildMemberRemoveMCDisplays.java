package events.discordevents.guildmemberremove;

import java.util.Locale;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRemoveAbstract;
import modules.MemberCountDisplay;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;

@DiscordEvent(allowBannedUser = true, allowBots = true)
public class GuildMemberRemoveMCDisplays extends GuildMemberRemoveAbstract {

    @Override
    public boolean onGuildMemberRemove(GuildMemberRemoveEvent event, EntityManagerWrapper entityManager) {
        Locale locale = entityManager.findGuildEntity(event.getGuild().getIdLong()).getLocale();
        MemberCountDisplay.manage(locale, event.getGuild());
        return true;
    }

}
