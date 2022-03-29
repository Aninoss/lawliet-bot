package events.discordevents.guildmemberupdatetimeout;

import core.CustomObservableMap;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberUpdateTimeOutAbstract;
import mysql.modules.servermute.DBServerMute;
import mysql.modules.servermute.ServerMuteData;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateTimeOutEvent;

@DiscordEvent(allowBannedUser = true, allowBots = true)
public class GuildMemberUpdateTimeoutMute extends GuildMemberUpdateTimeOutAbstract {

    @Override
    public boolean onGuildMemberUpdateTimeOutAbstract(GuildMemberUpdateTimeOutEvent event) {
        CustomObservableMap<Long, ServerMuteData> serverMuteMap = DBServerMute.getInstance().retrieve(event.getGuild().getIdLong());
        if (event.getNewTimeOutEnd() == null &&
                serverMuteMap.containsKey(event.getUser().getIdLong()) &&
                serverMuteMap.get(event.getUser().getIdLong()).isNewMethod()
        ) {
            serverMuteMap.remove(event.getUser().getIdLong());
        }
        return true;
    }

}
