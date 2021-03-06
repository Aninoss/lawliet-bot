package events.discordevents.guildmemberjoin;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import modules.MemberCountDisplay;
import mysql.modules.server.DBServer;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;

@DiscordEvent(allowBannedUser = true, allowBots = true)
public class GuildMemberJoinMCDisplays extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(ServerMemberJoinEvent event) throws Throwable {
        MemberCountDisplay.getInstance().manage(DBServer.getInstance().retrieve(event.getServer().getId()).getLocale(), event.getServer());
        return true;
    }
    
}
