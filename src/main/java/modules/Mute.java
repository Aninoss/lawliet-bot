package modules;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import core.CustomObservableMap;
import core.MemberCacheController;
import modules.schedulers.ServerMuteScheduler;
import mysql.modules.moderation.DBModeration;
import mysql.modules.moderation.ModerationData;
import mysql.modules.servermute.DBServerMute;
import mysql.modules.servermute.ServerMuteData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

public class Mute {

    public static void mute(Guild guild, User target, long minutes, String reason) {
        Instant expirationMax = Instant.now().plus(Duration.ofDays(27));
        Instant expiration = minutes > 0 ? Instant.now().plus(Duration.ofMinutes(minutes)) : null;

        ServerMuteData serverMuteData = new ServerMuteData(guild.getIdLong(), target.getIdLong(), expiration, true);
        DBServerMute.getInstance().retrieve(guild.getIdLong()).put(target.getIdLong(), serverMuteData);
        ServerMuteScheduler.loadServerMute(serverMuteData);

        Member member = MemberCacheController.getInstance().loadMember(guild, target.getIdLong()).join();
        if (member != null) {
            Instant rawExpiration = expiration;
            if (rawExpiration == null || rawExpiration.isAfter(expirationMax)) {
                rawExpiration = expirationMax;
            }
            member.timeoutUntil(rawExpiration)
                    .reason(reason)
                    .queue();
        }
    }

    public static void unmute(Guild guild, User target, String reason) {
        CustomObservableMap<Long, ServerMuteData> serverMuteMap = DBServerMute.getInstance().retrieve(guild.getIdLong());
        if (serverMuteMap.containsKey(target.getIdLong())) {
            boolean newMethod = serverMuteMap.get(target.getIdLong()).isNewMethod();
            if (newMethod) {
                serverMuteMap.remove(target.getIdLong());
                Member member = MemberCacheController.getInstance().loadMember(guild, target.getIdLong()).join();
                if (member != null) {
                    member.removeTimeout()
                            .reason(reason)
                            .queue();
                }
            } else {
                ModerationData moderationBean = DBModeration.getInstance().retrieve(guild.getIdLong());
                if (prerequisites(guild, moderationBean)) {
                    serverMuteMap.remove(target.getIdLong());
                    Member member = MemberCacheController.getInstance().loadMember(guild, target.getIdLong()).join();
                    if (member != null) {
                        moderationBean.getMuteRole().ifPresent(muteRole -> {
                            guild.removeRoleFromMember(member, muteRole)
                                    .reason(reason)
                                    .queue();
                        });
                    }
                }
            }
        }
    }

    private static boolean prerequisites(Guild guild, ModerationData moderationBean) {
        Optional<Role> muteRoleOpt = moderationBean.getMuteRole();
        return muteRoleOpt.isPresent() && guild.getSelfMember().canInteract(muteRoleOpt.get());
    }

}
