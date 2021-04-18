package modules;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import modules.schedulers.ServerMuteScheduler;
import mysql.modules.moderation.DBModeration;
import mysql.modules.moderation.ModerationData;
import mysql.modules.servermute.DBServerMute;
import mysql.modules.servermute.ServerMuteData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

public class Mute {

    public static void mute(Guild guild, User target, long minutes, String reason) {
        ModerationData moderationBean = DBModeration.getInstance().retrieve(guild.getIdLong());
        if (prerequisites(guild, moderationBean)) {
            Instant expiration = minutes > 0 ? Instant.now().plus(Duration.ofMinutes(minutes)) : null;
            ServerMuteData serverMuteData = new ServerMuteData(guild.getIdLong(), target.getIdLong(), expiration);
            DBServerMute.getInstance().retrieve(guild.getIdLong()).put(target.getIdLong(), serverMuteData);
            ServerMuteScheduler.getInstance().loadServerMute(serverMuteData);

            Optional.ofNullable(guild.getMemberById(target.getIdLong())).ifPresent(member -> {
                moderationBean.getMuteRole().ifPresent(muteRole -> {
                    AuditableRestAction<Void> restAction = guild.addRoleToMember(member, muteRole);
                    restAction.reason(reason)
                            .queue();
                });
            });
        }
    }

    public static void unmute(Guild guild, User target, String reason) {
        ModerationData moderationBean = DBModeration.getInstance().retrieve(guild.getIdLong());
        if (prerequisites(guild, moderationBean)) {
            DBServerMute.getInstance().retrieve(guild.getIdLong())
                    .remove(target.getIdLong());

            Optional.ofNullable(guild.getMemberById(target.getIdLong())).ifPresent(member -> {
                moderationBean.getMuteRole().ifPresent(muteRole -> {
                    AuditableRestAction<Void> restAction = guild.removeRoleFromMember(member, muteRole);
                    restAction.reason(reason)
                            .queue();
                });
            });
        }
    }

    private static boolean prerequisites(Guild guild, ModerationData moderationBean) {
        Optional<Role> muteRoleOpt = moderationBean.getMuteRole();
        return muteRoleOpt.isPresent() && guild.getSelfMember().canInteract(muteRoleOpt.get());
    }

}
