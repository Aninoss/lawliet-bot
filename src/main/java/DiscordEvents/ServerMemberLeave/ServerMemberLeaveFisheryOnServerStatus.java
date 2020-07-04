package DiscordEvents.ServerMemberLeave;

import Constants.FisheryStatus;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerMemberLeaveAbstract;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryServerBean;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEventAnnotation
public class ServerMemberLeaveFisheryOnServerStatus extends ServerMemberLeaveAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerMemberLeaveFisheryOnServerStatus.class);

    @Override
    public boolean onServerMemberLeave(ServerMemberLeaveEvent event) throws Throwable {
        Server server = event.getServer();

        FisheryServerBean fisheryServerBean = DBFishery.getInstance().getBean(server.getId());
        if (fisheryServerBean.getServerBean().getFisheryStatus() == FisheryStatus.STOPPED)
            return true;

        fisheryServerBean.getUserBean(event.getUser().getId()).setOnServer(false);

        return true;
    }

}
