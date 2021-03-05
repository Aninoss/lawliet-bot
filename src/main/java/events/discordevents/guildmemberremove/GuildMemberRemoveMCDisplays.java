package events.discordevents.guildmemberremove;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRemoveAbstract;
import modules.MemberCountDisplay;
import mysql.modules.server.DBServer;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;

import java.util.Locale;

@DiscordEvent(allowBannedUser = true, allowBots = true)
public class GuildMemberRemoveMCDisplays extends GuildMemberRemoveAbstract {

    @Override
    public boolean onGuildMemberRemove(ServerMemberLeaveEvent event) throws Throwable {
        Server server = event.getServer();
        Locale locale = DBServer.getInstance().getBean(server.getId()).getLocale();

        MemberCountDisplay.getInstance().manage(locale, server);
        return true;
    }

}
