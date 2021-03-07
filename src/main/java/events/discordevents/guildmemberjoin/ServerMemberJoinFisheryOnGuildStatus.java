package events.discordevents.guildmemberjoin;

import constants.FisheryStatus;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildBean;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

@DiscordEvent
public class ServerMemberJoinFisheryOnGuildStatus extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event) throws Throwable {
        FisheryGuildBean fisheryGuildBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong());
        if (fisheryGuildBean.getGuildBean().getFisheryStatus() == FisheryStatus.STOPPED)
            return true;

        fisheryGuildBean.getMemberBean(event.getUser().getIdLong())
                .setOnServer(true);
        return true;
    }
    
}
