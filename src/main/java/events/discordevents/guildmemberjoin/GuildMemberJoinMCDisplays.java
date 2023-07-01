package events.discordevents.guildmemberjoin;

import java.util.Locale;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import modules.MemberCountDisplay;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

@DiscordEvent(allowBannedUser = true, allowBots = true)
public class GuildMemberJoinMCDisplays extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event, EntityManagerWrapper entityManager) throws Throwable {
        Locale locale = entityManager.findGuildEntity(event.getGuild().getIdLong()).getLocale();
        MemberCountDisplay.manage(locale, event.getGuild());
        return true;
    }

}
