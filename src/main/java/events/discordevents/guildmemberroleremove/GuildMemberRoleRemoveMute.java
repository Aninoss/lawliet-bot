package events.discordevents.guildmemberroleremove;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRoleRemoveAbstract;
import mysql.modules.moderation.DBModeration;
import mysql.modules.servermute.DBServerMute;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;

@DiscordEvent(allowBannedUser = true, allowBots = true)
public class GuildMemberRoleRemoveMute extends GuildMemberRoleRemoveAbstract {

    @Override
    public boolean onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) throws Throwable {
        DBModeration.getInstance().retrieve(event.getGuild().getIdLong()).getMuteRole().ifPresent(muteRole -> {
            if (event.getRoles().stream().anyMatch(r -> r.getIdLong() == muteRole.getIdLong())) {
                DBServerMute.getInstance().retrieve(event.getGuild().getIdLong()).remove(event.getMember().getIdLong());
            }
        });

        return true;
    }

}
