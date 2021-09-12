package events.discordevents.guildmemberjoin;

import java.util.Locale;
import commands.Category;
import core.MemberCacheController;
import core.TextManager;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import modules.invitetracking.InviteTracking;
import mysql.modules.invitetracking.DBInviteTracking;
import mysql.modules.invitetracking.InviteTrackingData;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

@DiscordEvent
public class GuildMemberJoinInviteTracking extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event) throws Throwable {
        InviteTrackingData inviteTrackingData = DBInviteTracking.getInstance().retrieve(event.getGuild().getIdLong());
        if (!event.getUser().isBot() && inviteTrackingData.isActive()) {
            InviteTracking.registerMemberJoin(event.getMember())
                    .thenAccept(userId -> sendLog(inviteTrackingData, event.getMember(), userId))
                    .exceptionally(e -> {
                        //ignore
                        sendLog(inviteTrackingData, event.getMember(), null);
                        return null;
                    });
        }
        return true;
    }

    private void sendLog(InviteTrackingData inviteTrackingData, Member member, Long inviterUserId) {
        inviteTrackingData.getTextChannel().ifPresent(channel -> {
            Locale locale = inviteTrackingData.getGuildData().getLocale();
            String inviterTag = "";
            if (inviterUserId != null) {
                Member m = MemberCacheController.getInstance().loadMember(member.getGuild(), inviterUserId).join();
                inviterTag = m.getUser().getAsTag();
            }

            channel.sendMessage(TextManager.getString(locale, Category.UTILITY, "invitetracking_log", inviterUserId != null, member.getAsMention(), inviterTag))
                    .queue();
        });
    }

}
