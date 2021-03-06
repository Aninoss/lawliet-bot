package events.discordevents.guildmemberjoin;

import constants.FisheryStatus;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildBean;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;

@DiscordEvent
public class ServerMemberJoinFisheryOnGuildStatus extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(ServerMemberJoinEvent event) throws Throwable {
        Server server = event.getServer();

        FisheryGuildBean fisheryGuildBean = DBFishery.getInstance().retrieve(server.getId());
        if (fisheryGuildBean.getGuildBean().getFisheryStatus() == FisheryStatus.STOPPED)
            return true;

        fisheryGuildBean.getUserBean(event.getUser().getId()).setOnServer(true);

        return true;
    }
    
}
