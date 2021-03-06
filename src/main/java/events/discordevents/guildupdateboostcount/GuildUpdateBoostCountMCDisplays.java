package events.discordevents.guildupdateboostcount;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildUpdateBoostCountAbstract;
import modules.MemberCountDisplay;
import mysql.modules.server.DBServer;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateBoostCountEvent;
import java.util.Locale;

@DiscordEvent()
public class GuildUpdateBoostCountMCDisplays extends GuildUpdateBoostCountAbstract {

    @Override
    public boolean onGuildUpdateBoostCount(GuildUpdateBoostCountEvent event) throws Throwable {
        Locale locale = DBServer.getInstance().retrieve(event.getGuild().getIdLong()).getLocale();
        MemberCountDisplay.getInstance().manage(locale, event.getGuild());
        return true;
    }

}
