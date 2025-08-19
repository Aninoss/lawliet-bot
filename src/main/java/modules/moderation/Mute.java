package modules.moderation;

import core.CustomObservableMap;
import core.MemberCacheController;
import core.utils.BotPermissionUtil;
import modules.schedulers.ServerMuteScheduler;
import mysql.modules.servermute.DBServerMute;
import mysql.modules.servermute.ServerMuteData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.time.Duration;
import java.time.Instant;

public class Mute {

    public static void mute(Guild guild, User target, Integer durationMinutes, String reason) {
        Instant expirationMax = Instant.now().plus(Duration.ofDays(27));
        Instant expiration = durationMinutes != null ? Instant.now().plus(Duration.ofMinutes(durationMinutes)) : null;

        ServerMuteData serverMuteData = new ServerMuteData(guild.getIdLong(), target.getIdLong(), expiration, true);
        DBServerMute.getInstance().retrieve(guild.getIdLong()).put(target.getIdLong(), serverMuteData);
        ServerMuteScheduler.loadServerMute(serverMuteData);

        Member member = MemberCacheController.getInstance().loadMember(guild, target.getIdLong()).join();
        if (member != null) {
            Instant rawExpiration = expiration;
            if (rawExpiration == null || rawExpiration.isAfter(expirationMax)) {
                rawExpiration = expirationMax;
            }
            if (!BotPermissionUtil.can(member, Permission.ADMINISTRATOR)) {
                member.timeoutUntil(rawExpiration)
                        .reason(reason)
                        .queue();
            }
        }
    }

    public static void unmute(Guild guild, User target, String reason) {
        CustomObservableMap<Long, ServerMuteData> serverMuteMap = DBServerMute.getInstance().retrieve(guild.getIdLong());
        if (serverMuteMap.containsKey(target.getIdLong())) {
            serverMuteMap.remove(target.getIdLong());
            Member member = MemberCacheController.getInstance().loadMember(guild, target.getIdLong()).join();
            if (member != null && !BotPermissionUtil.can(member, Permission.ADMINISTRATOR)) {
                member.removeTimeout()
                        .reason(reason)
                        .queue();
            }
        }
    }

}
