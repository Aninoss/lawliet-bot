package events.discordevents.guildupdateboostcount;

import java.util.Locale;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildUpdateBoostCountAbstract;
import modules.MemberCountDisplay;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateBoostCountEvent;

@DiscordEvent
public class GuildUpdateBoostCountMCDisplays extends GuildUpdateBoostCountAbstract {

    @Override
    public boolean onGuildUpdateBoostCount(GuildUpdateBoostCountEvent event) throws Throwable {
        Locale locale = DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).getLocale();
        MemberCountDisplay.manage(locale, event.getGuild());
        return true;
    }

}
