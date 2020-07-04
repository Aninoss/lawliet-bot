package DiscordEvents.ServerMemberJoin;

import Constants.FisheryStatus;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerMemberJoinAbstract;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryServerBean;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEventAnnotation
public class ServerMemberJoinFisheryOnServerStatus extends ServerMemberJoinAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerMemberJoinFisheryOnServerStatus.class);

    @Override
    public boolean onServerMemberJoin(ServerMemberJoinEvent event) throws Throwable {
        Server server = event.getServer();

        FisheryServerBean fisheryServerBean = DBFishery.getInstance().getBean(server.getId());
        if (fisheryServerBean.getServerBean().getFisheryStatus() == FisheryStatus.STOPPED)
            return true;

        fisheryServerBean.getUserBean(event.getUser().getId()).setOnServer(true);

        return true;
    }
    
}
