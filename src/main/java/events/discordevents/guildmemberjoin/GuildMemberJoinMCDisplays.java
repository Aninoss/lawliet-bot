package events.discordevents.guildmemberjoin;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import modules.MemberCountDisplay;
import mysql.modules.server.DBServer;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

@DiscordEvent(allowBannedUser = true, allowBots = true)
public class GuildMemberJoinMCDisplays extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event) throws Throwable {
        MemberCountDisplay.getInstance()
                .manage(DBServer.getInstance().retrieve(event.getGuild().getIdLong()).getLocale(), event.getGuild());
        return true;
    }
    
}
