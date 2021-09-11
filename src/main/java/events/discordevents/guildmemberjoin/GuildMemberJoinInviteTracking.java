package events.discordevents.guildmemberjoin;

import core.MainLogger;
import core.Program;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import modules.invitetracking.InviteTracking;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

@DiscordEvent
public class GuildMemberJoinInviteTracking extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event) throws Throwable {
        if (!event.getUser().isBot() && DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).isInviteTracking()) {
            InviteTracking.registerMemberJoin(event.getMember()).thenAccept(userId -> {
                if (!Program.productionMode()) {
                    MainLogger.get().info("Member {} invited by {}", event.getMember().getUser().getAsTag(), userId);
                }
            });
        }
        return true;
    }

}
