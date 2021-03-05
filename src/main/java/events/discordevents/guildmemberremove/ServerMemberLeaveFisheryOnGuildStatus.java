package events.discordevents.guildmemberremove;

import constants.FisheryStatus;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRemoveAbstract;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryServerBean;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;

@DiscordEvent
public class ServerMemberLeaveFisheryOnGuildStatus extends GuildMemberRemoveAbstract {

    @Override
    public boolean onGuildMemberRemove(ServerMemberLeaveEvent event) throws Throwable {
        Server server = event.getServer();

        FisheryServerBean fisheryServerBean = DBFishery.getInstance().getBean(server.getId());
        if (fisheryServerBean.getServerBean().getFisheryStatus() == FisheryStatus.STOPPED)
            return true;

        fisheryServerBean.getUserBean(event.getUser().getId()).setOnServer(false);

        return true;
    }

}
