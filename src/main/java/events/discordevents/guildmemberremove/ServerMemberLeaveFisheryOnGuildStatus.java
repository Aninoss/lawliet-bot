package events.discordevents.guildmemberremove;

import constants.FisheryStatus;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRemoveAbstract;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildBean;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;

@DiscordEvent
public class ServerMemberLeaveFisheryOnGuildStatus extends GuildMemberRemoveAbstract {

    @Override
    public boolean onGuildMemberRemove(ServerMemberLeaveEvent event) throws Throwable {
        Server server = event.getServer();

        FisheryGuildBean fisheryGuildBean = DBFishery.getInstance().retrieve(server.getId());
        if (fisheryGuildBean.getGuildBean().getFisheryStatus() == FisheryStatus.STOPPED)
            return true;

        fisheryGuildBean.getUserBean(event.getUser().getId()).setOnServer(false);

        return true;
    }

}
