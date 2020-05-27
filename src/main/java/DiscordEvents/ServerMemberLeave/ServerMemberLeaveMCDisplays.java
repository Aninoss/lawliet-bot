package DiscordEvents.ServerMemberLeave;

import Commands.ManagementCategory.MemberCountDisplayCommand;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerMemberLeaveAbstract;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;

import java.util.Locale;

@DiscordEventAnnotation
public class ServerMemberLeaveMCDisplays extends ServerMemberLeaveAbstract {

    @Override
    public boolean onServerMemberLeave(ServerMemberLeaveEvent event) throws Throwable {
        Server server = event.getServer();
        Locale locale = DBServer.getInstance().getBean(server.getId()).getLocale();

        MemberCountDisplayCommand.manage(locale, server);
        return true;
    }

}
