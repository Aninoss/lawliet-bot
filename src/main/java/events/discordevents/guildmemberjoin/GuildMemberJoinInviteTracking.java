package events.discordevents.guildmemberjoin;

import java.util.Collections;
import java.util.Locale;
import commands.Category;
import core.ShardManager;
import core.TextManager;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import modules.invitetracking.InviteTracking;
import mysql.modules.invitetracking.DBInviteTracking;
import mysql.modules.invitetracking.InviteTrackingData;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

@DiscordEvent(allowBots = true)
public class GuildMemberJoinInviteTracking extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event) throws Throwable {
        InviteTrackingData inviteTrackingData = DBInviteTracking.getInstance().retrieve(event.getGuild().getIdLong());
        if (inviteTrackingData.isActive()) {
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
            int n = 0;
            if (inviterUserId != null) {
                if (inviterUserId > 0) {
                    User user = ShardManager.fetchUserById(inviterUserId)
                            .exceptionally(e -> null)
                            .join();
                    if (user != null) {
                        inviterTag = user.getAsTag();
                        n = 2;
                    }
                } else {
                    n = 1;
                }
            }

            MessageAction messageAction = channel.sendMessage(TextManager.getString(locale, Category.UTILITY, "invitetracking_log", n, member.getAsMention(), inviterTag));
            if (!inviteTrackingData.getPing()) {
                messageAction = messageAction.allowedMentions(Collections.emptySet());
            }
            messageAction.queue();
        });
    }

}
